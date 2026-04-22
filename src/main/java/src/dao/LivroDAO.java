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
import src.util.OrdenacaoExterna;

/**
 * DAO de Livros.
 *
 * * Índice B+: chave = id, valor = offset físico no livros.bin.
 *
 * INTEGRIDADE REFERENCIAL:
 * - excluirLivro() rejeita a exclusão se houver vínculos em livros_autores,
 *   e remove em cascata todos os vínculos em tags_livros antes de deletar.
 * - excluirLivroEmCascata() remove todos os vínculos (autores E tags) e depois exclui.
 * - O método buscarLivrosPorEditora() é utilizado pelo EditoraDAO para
 *   verificar dependências antes de excluir uma editora.
 */
public class LivroDAO {

    private final Arquivo<Livro> arqLivros;
    private final ArvoreBMais    indiceId;         // B+: listagem ordenada / intervalo
    private final HashExtensivel hashId;           // Hash: busca direta por PK em O(1)
    private final ArvoreBMais    indicePorEditora; // B+: relacionamento 1:N (Editora→Livros)

    // Dependências injetadas para verificação de integridade referencial
    private LivroAutorDAO livroAutorDAO;
    private TagsLivrosDAO tagsLivrosDAO; // NOVO — necessário para cascata de tags

    public LivroDAO() throws Exception {
        arqLivros        = new Arquivo<>("livros", Livro.class.getConstructor());
        indiceId         = new ArvoreBMais("livros_id");
        hashId           = new HashExtensivel("livros_id");
        indicePorEditora = new ArvoreBMais("livros_por_editora");
    }

    public void setLivroAutorDAO(LivroAutorDAO dao) {
        this.livroAutorDAO = dao;
    }

    /** Injeta o TagsLivrosDAO para garantir a cascata de tags na exclusão de livros. */
    public void setTagsLivrosDAO(TagsLivrosDAO dao) {
        this.tagsLivrosDAO = dao;
    }

    // -------------------------------------------------------------------------
    // CREATE
    // -------------------------------------------------------------------------

    public int incluirLivro(Livro livro) throws Exception {
        CreateResult cr = arqLivros.create(livro);
        if (cr.id > 0) {
            indiceId.inserir(cr.id, cr.endereco);
            hashId.inserir(cr.id, cr.endereco);
            // Índice 1:N: chave composta (idEditora << 16 | idLivro) agrupa livros por editora
            indicePorEditora.inserir(chaveEditora(livro.getIdEditora(), cr.id), cr.endereco);
        }
        return cr.id;
    }

    // -------------------------------------------------------------------------
    // READ
    // -------------------------------------------------------------------------

    public Livro buscarLivroPorId(int id) throws Exception {
        long offset = hashId.buscar(id); // Hash: O(1) amortizado
        if (offset != ArvoreBMais.NULO) {
            return arqLivros.readByOffset(offset);
        }
        return arqLivros.read(id); // Fallback sequencial de segurança
    }

    /**
     * Busca sequencial por ISBN.
     * Nota (Fase 2): Feito através de scan linear pois não há índice secundário.
     */
    public Livro buscarLivroPorIsbn(String isbn) throws Exception {
        for (Livro l : arqLivros.listarTodos()) {
            if (new String(l.getIsbn()).trim().equals(isbn.trim())) {
                return l;
            }
        }
        return null;
    }

    /**
     * Busca sequencial por Título.
     * Nota (Fase 2): Feito através de scan linear pois não há índice secundário.
     */
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

    /**
     * Retorna todos os livros de uma editora via índice B+ secundário (1:N).
     * Usa range scan na B+ sobre chaves compostas (idEditora << 16 | idLivro),
     * sem varrer o arquivo principal — O(log n + k) onde k = número de livros da editora.
     */
    public List<Livro> buscarLivrosPorEditora(int idEditora) throws Exception {
        List<Livro> resultado = new ArrayList<>();
        long chaveMin = (long) idEditora << 16;
        long chaveMax = (long) (idEditora + 1) << 16;

        long[][] pares = indicePorEditora.listarOrdenado();
        for (long[] par : pares) {
            if (par[0] >= chaveMin && par[0] < chaveMax) {
                Livro l = arqLivros.readByOffset(par[1]);
                if (l != null) resultado.add(l);
            }
        }
        return resultado;
    }

    // -------------------------------------------------------------------------
    // UPDATE
    // -------------------------------------------------------------------------

    public boolean alterarLivro(Livro livro) throws Exception {
        // Guarda o idEditora ANTES do update para poder remover a chave composta antiga
        Livro antigo = buscarLivroPorId(livro.getId());
        boolean ok = arqLivros.update(livro);
        if (ok) {
            if (antigo != null && antigo.getIdEditora() != livro.getIdEditora()) {
                // Editora mudou — remove índice antigo e insere o novo após reindexar
                indicePorEditora.remover(chaveEditora(antigo.getIdEditora(), livro.getId()));
                reindexar(livro.getId());
                long offsetAtual = hashId.buscar(livro.getId());
                if (offsetAtual != ArvoreBMais.NULO)
                    indicePorEditora.inserir(chaveEditora(livro.getIdEditora(), livro.getId()), offsetAtual);
            } else {
                // Editora não mudou: reindexar atualiza todos os índices normalmente
                reindexar(livro.getId());
            }
        }
        return ok;
    }

    // -------------------------------------------------------------------------
    // DELETE — com integridade referencial completa
    // -------------------------------------------------------------------------

    /**
     * Exclui um livro com integridade referencial:
     * - Rejeita se houver autores vinculados em livros_autores.
     * - Remove em cascata todos os vínculos em tags_livros antes de deletar.
     */
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
                indicePorEditora.remover(chaveEditora(livro.getIdEditora(), id));
        }
        return ok;
    }

    /**
     * Exclui o livro E todos os seus vínculos com autores E tags (Delete em Cascata total).
     * Use este método quando a interface quiser forçar a exclusão sem verificar dependentes.
     */
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
                indicePorEditora.remover(chaveEditora(livro.getIdEditora(), id));
        }
        return ok;
    }

    // -------------------------------------------------------------------------
    // LISTAGEM ORDENADA (travessia das folhas da B+)
    // -------------------------------------------------------------------------

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

    /**
     * Ordenação por Intercalação — exigência do enunciado (Fase 2, item 3d-1).
     * Usa OrdenacaoExterna para ordenar por título sem carregar
     * todos os registros na RAM de uma vez.
     */
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

    /**
     * Listagem em ordem DECRESCENTE por ID via travessia da Árvore B+.
     */
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

    // -------------------------------------------------------------------------
    // Auxiliares
    // -------------------------------------------------------------------------

    /**
     * Chave composta para o índice 1:N: agrupa livros por editora dentro da B+.
     */
    private static int chaveEditora(int idEditora, int idLivro) {
        return (idEditora << 16) | (idLivro & 0xFFFF);
    }

    /**
     * Verifica se existem autores vinculados ao livro.
     * Lança IllegalStateException se houver dependentes, forçando o chamador
     * a usar excluirLivroEmCascata() ou remover os vínculos manualmente.
     */
    private void verificarDependentesAutores(int id) throws Exception {
        if (livroAutorDAO != null && !livroAutorDAO.buscarAutoresDoLivro(id).isEmpty()) {
            throw new IllegalStateException(
                "O Livro ID " + id + " possui autores vinculados. " +
                "Use excluirLivroEmCascata() ou remova os vínculos manualmente primeiro.");
        }
    }

    /**
     * Varre o ficheiro para encontrar o novo offset físico após um update.
     * Atualiza os três índices: indiceId, hashId e indicePorEditora.
     */
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
                    int idEditoraLido = dis.readInt(); // segundo campo serializado em Livro

                    if (idLido == id) {
                        indiceId.atualizar(id, pos);
                        hashId.atualizar(id, pos);
                        indicePorEditora.atualizar(chaveEditora(idEditoraLido, id), pos);
                        return;
                    }
                } else {
                    raf.skipBytes(tamFisico);
                }
            }
        }
    }
}