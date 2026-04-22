package src.dao;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import src.model.LivroAutor;
import src.util.Arquivo;
import src.util.Arquivo.CreateResult;
import src.util.ArvoreBMais;
import src.util.HashExtensivel;

/**
 * DAO da tabela intermédia livros_autores (N:N entre Livros e Autores).
 *
 * Índices mantidos:
 * - indiceId       → B+ por PK (id → offset)       para busca direta.
 * - indicePorLivro → B+ por idLivro (idLivro → offset) para buscar todos os autores de um livro.
 * - indicePorAutor → B+ por idAutor (idAutor → offset) para buscar todos os livros de um autor.
 *
 * LIMITAÇÃO CONHECIDA (Fase 2):
 * A Árvore B+ não suporta chaves duplicadas. Para os índices secundários,
 * a chave utilizada é uma composição matemática:
 * chave = idEstrangeiro * MAX_ID + id (garantindo unicidade absoluta).
 * A pesquisa percorre as folhas (range scan) e filtra pelos IDs correspondentes.
 */
public class LivroAutorDAO {

    // Constante base para gerar chaves compostas (suficiente para as restrições de int)
    private static final int MAX_ID = 100_000;

    private final Arquivo<LivroAutor> arqLivrosAutores;
    private final ArvoreBMais         indiceId;       // B+: listagem e range scan
    private final HashExtensivel      hashId;         // Hash: busca direta por PK
    private final ArvoreBMais         indicePorLivro;
    private final ArvoreBMais         indicePorAutor;

    public LivroAutorDAO() throws Exception {
        arqLivrosAutores = new Arquivo<>("livros_autores", LivroAutor.class.getConstructor());
        indiceId         = new ArvoreBMais("livros_autores_id");
        hashId           = new HashExtensivel("livros_autores_id");
        indicePorLivro   = new ArvoreBMais("livros_autores_por_livro");
        indicePorAutor   = new ArvoreBMais("livros_autores_por_autor");
    }

    // -------------------------------------------------------------------------
    // CREATE (Vincular)
    // -------------------------------------------------------------------------

    /**
     * Cria o vínculo N:N e insere nos TRÊS índices B+.
     */
    public boolean vincularAutorAoLivro(int idLivro, int idAutor) throws Exception {
        LivroAutor novo = new LivroAutor(idLivro, idAutor);
        CreateResult cr = arqLivrosAutores.create(novo);

        if (cr.id > 0) {
            indiceId.inserir(cr.id, cr.endereco);
            hashId.inserir(cr.id, cr.endereco);
            indicePorLivro.inserir((int) chaveComposta(idLivro, cr.id), cr.endereco);
            indicePorAutor.inserir((int) chaveComposta(idAutor, cr.id), cr.endereco);
            return true;
        }
        return false;
    }

    // -------------------------------------------------------------------------
    // READ
    // -------------------------------------------------------------------------

    public LivroAutor buscarPorId(int id) throws Exception {
        long offset = hashId.buscar(id); // Hash: O(1)
        if (offset != ArvoreBMais.NULO) {
            return arqLivrosAutores.readByOffset(offset);
        }
        return arqLivrosAutores.read(id);
    }

    public List<LivroAutor> listarTodos() throws Exception {
        return arqLivrosAutores.listarTodos();
    }

    /** Busca rápida usando a Árvore B+ (Range Scan). */
    public List<LivroAutor> buscarAutoresDoLivro(int idLivro) throws Exception {
        return buscarPorIndiceSecundario(
            indicePorLivro,
            idLivro,
            la -> la.getIdLivro() == idLivro
        );
    }

    /** Busca rápida usando a Árvore B+ (Range Scan). */
    public List<LivroAutor> buscarLivrosDoAutor(int idAutor) throws Exception {
        return buscarPorIndiceSecundario(
            indicePorAutor,
            idAutor,
            la -> la.getIdAutor() == idAutor
        );
    }

    // -------------------------------------------------------------------------
    // DELETE
    // -------------------------------------------------------------------------

    /**
     * Remove um vínculo e limpa os TRÊS índices.
     */
    public boolean excluirPorId(int id) throws Exception {
        LivroAutor la = buscarPorId(id);
        if (la == null) return false;

        boolean ok = arqLivrosAutores.delete(id);
        if (ok) {
            indiceId.remover(id);
            hashId.remover(id);
            indicePorLivro.remover((int) chaveComposta(la.getIdLivro(), id));
            indicePorAutor.remover((int) chaveComposta(la.getIdAutor(), id));
        }
        return ok;
    }

    public int excluirAutoresDoLivro(int idLivro) throws Exception {
        List<LivroAutor> registros = buscarAutoresDoLivro(idLivro);
        int removidos = 0;
        for (LivroAutor la : registros) {
            if (excluirPorId(la.getId())) removidos++;
        }
        return removidos;
    }

    // -------------------------------------------------------------------------
    // Auxiliares (Gestão de Índices e Range Scans)
    // -------------------------------------------------------------------------

    /**
     * Percorre as folhas do índice secundário B+ à procura de chaves que
     * se enquadrem no intervalo matemático da chave composta.
     */
    private List<LivroAutor> buscarPorIndiceSecundario(
            ArvoreBMais indice,
            int         idFiltro,
            Predicate<LivroAutor> predicado) throws Exception {

        long[][] pares = indice.listarOrdenado();
        long chaveMin  = chaveComposta(idFiltro, 0);
        long chaveMax  = chaveComposta(idFiltro + 1, 0);

        List<LivroAutor> resultado = new ArrayList<>();
        for (long[] par : pares) {
            if (par[0] >= chaveMin && par[0] < chaveMax) {
                LivroAutor la = arqLivrosAutores.readByOffset(par[1]);
                if (la != null && predicado.test(la)) {
                    resultado.add(la);
                }
            }
        }
        return resultado;
    }

    /** Gera a chave composta única: base * MAX_ID + seq */
    private static long chaveComposta(int base, int seq) {
        return ((long) base * MAX_ID) + seq;
    }

    /**
     * Reindexa os três índices caso o bloco mude de offset (Ex: após um update estrutural).
     * Otimizado para saltar bytes de blocos excluídos (skipBytes).
     */
    public void reindexar(int id) throws Exception {
        LivroAutor antigo = buscarPorId(id);
        if (antigo == null) return;

        try (RandomAccessFile raf = new RandomAccessFile("./data/livros_autores.bin", "r")) {
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
                    
                    int idLido = dis.readInt();
                    
                    if (idLido == id) {
                        indiceId.atualizar(id, pos);
                        hashId.atualizar(id, pos);
                        indicePorLivro.atualizar((int) chaveComposta(antigo.getIdLivro(), id), pos);
                        indicePorAutor.atualizar((int) chaveComposta(antigo.getIdAutor(), id), pos);
                        return;
                    }
                } else {
                    raf.skipBytes(tamFisico); // Otimização de RAM
                }
            }
        }
    }
}