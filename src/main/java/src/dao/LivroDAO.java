package src.dao;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;

import src.model.Livro;
import src.util.Arquivo;
import src.util.Arquivo.CreateResult;
import src.util.ArvoreBMais;
import src.util.HashExtensivel;
import src.util.HashExtensivelLong;
import src.util.OrdenacaoExterna;


public class LivroDAO {

    private final Arquivo<Livro>     arqLivros;
    private final ArvoreBMais        indiceId;        // B+ para listagem ordenada por PK
    private final HashExtensivel     hashId;           // Hash extensível para busca O(1) por PK
    private final HashExtensivelLong hashPorEditora;   // Hash extensível 1:N: chave composta (idEditora, idLivro)

    private LivroAutorDAO livroAutorDAO;
    private TagsLivrosDAO tagsLivrosDAO;

    public LivroDAO() throws Exception {
        arqLivros      = new Arquivo<>("livros", Livro.class.getConstructor());
        indiceId       = new ArvoreBMais("livros_id");
        hashId         = new HashExtensivel("livros_id");
        hashPorEditora = new HashExtensivelLong("livros_por_editora");
    }

    public void setLivroAutorDAO(LivroAutorDAO dao) {
        this.livroAutorDAO = dao;
    }

    public void setTagsLivrosDAO(TagsLivrosDAO dao) {
        this.tagsLivrosDAO = dao;
    }

    // CREATE

    public int incluirLivro(Livro livro) throws Exception {
        // Validação de ISBN único
        if (buscarLivroPorIsbn(new String(livro.getIsbn()).trim()) != null) {
            throw new Exception("Já existe um livro cadastrado com este ISBN.");
        }

        CreateResult cr = arqLivros.create(livro);
        if (cr.id > 0) {
            indiceId.inserir(cr.id, cr.endereco);
            hashId.inserir(cr.id, cr.endereco);
            // Índice 1:N com Hash Extensível: chave composta (idEditora, idLivro) → offset do livro
            hashPorEditora.inserir(chaveEditora(livro.getIdEditora(), cr.id), cr.endereco);
        }
        return cr.id;
    }

    // READ

    public Livro buscarLivroPorId(int id) throws Exception {
        long offset = hashId.buscar(id); // Hash: O(1) amortizado
        if (offset != ArvoreBMais.NULO) {
            return arqLivros.readByOffset(offset);
        }
        return arqLivros.read(id); // Fallback sequencial de segurança
    }

    public Livro buscarLivroPorIsbn(String isbn) throws Exception {
        for (Livro l : arqLivros.listarTodos()) {
            if (new String(l.getIsbn()).trim().equals(isbn.trim())) {
                return l;
            }
        }
        return null;
    }


    public Livro buscarLivroPorTitulo(String titulo) throws Exception {
        for (Livro l : arqLivros.listarTodos()) {
            if (l.getTitulo().equalsIgnoreCase(titulo)) {
                return l;
            }
        }
        return null;
    }

    public List<Livro> listarTodos() throws Exception {
        return arqLivros.listarTodos();
    }


    public List<Livro> buscarLivrosPorEditora(int idEditora) throws Exception {
        List<Livro> resultado = new ArrayList<>();
        // Chave mínima: idEditora nos 32 bits superiores, 0 nos 32 bits inferiores
        long chaveMin = ((long) idEditora) << 32;
        // Chave máxima: (idEditora + 1) nos 32 bits superiores, 0 nos 32 bits inferiores
        long chaveMax = ((long) (idEditora + 1)) << 32;

        // Navegação 1:N via Hash Extensível: recupera todos os filhos do pai (idEditora)
        long[][] pares = hashPorEditora.listarPorFaixa(chaveMin, chaveMax);
        for (long[] par : pares) {
            Livro l = arqLivros.readByOffset(par[1]);
            if (l != null) resultado.add(l);
        }
        return resultado;
    }

    // UPDATE

    public boolean alterarLivro(Livro livro) throws Exception {
        // Validação de ISBN único (excluindo o próprio livro)
        Livro existente = buscarLivroPorIsbn(new String(livro.getIsbn()).trim());
        if (existente != null && existente.getId() != livro.getId()) {
            throw new Exception("Já existe outro livro cadastrado com este ISBN.");
        }

        // Guarda o idEditora ANTES do update para poder remover a chave composta antiga
        Livro antigo = buscarLivroPorId(livro.getId());
        boolean ok = arqLivros.update(livro);
        if (ok) {
            if (antigo != null && antigo.getIdEditora() != livro.getIdEditora()) {
                // Editora mudou — remove chave composta antiga e insere nova
                hashPorEditora.remover(chaveEditora(antigo.getIdEditora(), livro.getId()));
                reindexar(livro.getId());
                long offsetAtual = hashId.buscar(livro.getId());
                if (offsetAtual != ArvoreBMais.NULO)
                    hashPorEditora.inserir(chaveEditora(livro.getIdEditora(), livro.getId()), offsetAtual);
            } else {
                reindexar(livro.getId());
            }
        }
        return ok;
    }

    // DELETE — com integridade referencial completa

    public boolean excluirLivro(int id) throws Exception {
        // Bloqueia exclusão se houver autores vinculados
        verificarDependentesAutores(id);

        // Remove em cascata os vínculos de tags (não bloqueiam a exclusão)
        if (tagsLivrosDAO != null) {
            tagsLivrosDAO.excluirTagsDoLivro(id);
        }

        Livro livro = buscarLivroPorId(id); // precisa do idEditora antes de deletar
        boolean ok = arqLivros.delete(id);
        if (ok) {
            indiceId.remover(id);
            hashId.remover(id);
            if (livro != null)
                hashPorEditora.remover(chaveEditora(livro.getIdEditora(), id));
        }
        return ok;
    }

    public boolean excluirLivroEmCascata(int id) throws Exception {
        Livro livro = buscarLivroPorId(id); // precisa do idEditora antes de deletar

        // Remove todos os vínculos de autores
        if (livroAutorDAO != null) {
            livroAutorDAO.excluirAutoresDoLivro(id);
        }

        // Remove todos os vínculos de tags
        if (tagsLivrosDAO != null) {
            tagsLivrosDAO.excluirTagsDoLivro(id);
        }

        boolean ok = arqLivros.delete(id);
        if (ok) {
            indiceId.remover(id);
            hashId.remover(id);
            if (livro != null)
                hashPorEditora.remover(chaveEditora(livro.getIdEditora(), id));
        }
        return ok;
    }

    // LISTAGEM ORDENADA 

    public List<Livro> listarOrdenadoPorId() throws Exception {
        long[][] pares = indiceId.listarOrdenado();

        // Fallback: índice B+ vazio mas arquivo tem dados → reconstrói o índice
        if (pares.length == 0 && arqLivros.getTotalRegistros() > 0) {
            for (Arquivo.OffsetEntry<Livro> e : arqLivros.listarComOffset()) {
                indiceId.inserir(e.objeto.getId(), e.offset);
            }
            pares = indiceId.listarOrdenado();
        }

        List<Livro> res = new ArrayList<>();
        for (long[] par : pares) {
            Livro l = arqLivros.readByOffset(par[1]);
            if (l != null) res.add(l);
        }
        return res;
    }

    public List<Livro> listarOrdenadoPorTitulo() throws Exception {
        OrdenacaoExterna<Livro> ord = new OrdenacaoExterna<>(
            arqLivros,
            Livro.class.getConstructor(),
            (a, b) -> a.getTitulo().compareToIgnoreCase(b.getTitulo())
        );
        long[][] pares = ord.ordenar();
        List<Livro> res = new ArrayList<>();
        for (long[] par : pares) {
            Livro l = arqLivros.readByOffset(par[1]);
            if (l != null) res.add(l);
        }
        return res;
    }

    public List<Livro> listarOrdenadoDecrescentePorId() throws Exception {
        long[][] pares = indiceId.listarOrdenadoDecrescente();

        if (pares.length == 0 && arqLivros.getTotalRegistros() > 0) {
            for (Arquivo.OffsetEntry<Livro> e : arqLivros.listarComOffset()) {
                indiceId.inserir(e.objeto.getId(), e.offset);
            }
            pares = indiceId.listarOrdenadoDecrescente();
        }

        List<Livro> res = new ArrayList<>();
        for (long[] par : pares) {
            Livro l = arqLivros.readByOffset(par[1]);
            if (l != null) res.add(l);
        }
        return res;
    }

    // Auxiliares

    private static long chaveEditora(int idEditora, int idLivro) {
        return (((long) idEditora) << 32) | (idLivro & 0xFFFFFFFFL);
    }

    private void verificarDependentesAutores(int id) throws Exception {
        if (livroAutorDAO != null && !livroAutorDAO.buscarAutoresDoLivro(id).isEmpty()) {
            throw new IllegalStateException(
                "O Livro ID " + id + " possui autores vinculados. " +
                "Use excluirLivroEmCascata() ou remova os vínculos manualmente primeiro.");
        }
    }

    private void reindexar(int id) throws Exception {
        try (RandomAccessFile raf = new RandomAccessFile("./data/livros.bin", "r")) {
            raf.seek(Arquivo.TAM_CABECALHO);

            while (raf.getFilePointer() < raf.length()) {
                long    pos       = raf.getFilePointer();
                boolean lapide    = raf.readBoolean();
                int     tamFisico = raf.readInt();

                if (lapide) {
                    byte[] dados = new byte[tamFisico];
                    raf.readFully(dados);

                    ByteArrayInputStream bais = new ByteArrayInputStream(dados);
                    DataInputStream      dis  = new DataInputStream(bais);

                    int idLido        = dis.readInt();
                    int idEditoraLido = dis.readInt(); 
                    if (idLido == id) {
                        indiceId.atualizar(id, pos);
                        hashId.atualizar(id, pos);
                        hashPorEditora.atualizar(chaveEditora(idEditoraLido, id), pos);
                        return;
                    }
                } else {
                    raf.skipBytes(tamFisico);
                }
            }
        }
    }
}
