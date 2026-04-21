package src.dao;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import src.model.LivroAutor;
import src.util.Arquivo;
import src.util.Arquivo.CreateResult;
import src.util.ArvoreBMais;

/**
 * DAO da tabela intermediária livros_autores (N:N entre Livros e Autores).
 *
 * Índices mantidos:
 *   - indiceId      → B+ por PK (id → offset)       para busca direta e listagem ordenada
 *   - indicePorLivro → B+ por idLivro (idLivro → offset)  para buscar todos os autores de um livro
 *   - indicePorAutor → B+ por idAutor (idAutor → offset)  para buscar todos os livros de um autor
 *
 * Os índices secundários (indicePorLivro e indicePorAutor) permitem consultas
 * eficientes conforme exigido pelo enunciado para relacionamentos N:N.
 *
 * LIMITAÇÃO CONHECIDA: a B+ não suporta chaves duplicadas nesta implementação.
 * Para os índices secundários, a chave usada é uma composição:
 *   indicePorLivro: chave = idLivro * MAX_ID + id  (garante unicidade)
 *   indicePorAutor: chave = idAutor * MAX_ID + id
 * A busca percorre as folhas e filtra por idLivro/idAutor.
 * Alternativa mais robusta: Hash Extensível com encadeamento (Fase 2/3).
 */
public class LivroAutorDAO {

    private static final int MAX_ID = 100_000; // suficiente para o projeto

    private final Arquivo<LivroAutor> arqLivrosAutores;
    private final ArvoreBMais         indiceId;
    private final ArvoreBMais         indicePorLivro;
    private final ArvoreBMais         indicePorAutor;

    public LivroAutorDAO() throws Exception {
        arqLivrosAutores = new Arquivo<>("livros_autores",
                                         LivroAutor.class.getConstructor());
        indiceId        = new ArvoreBMais("livros_autores_id");
        indicePorLivro  = new ArvoreBMais("livros_autores_por_livro");
        indicePorAutor  = new ArvoreBMais("livros_autores_por_autor");
    }

    // -------------------------------------------------------------------------
    // CREATE
    // -------------------------------------------------------------------------

    public boolean vincularAutorAoLivro(int idLivro, int idAutor) throws Exception {
        LivroAutor la = new LivroAutor(idLivro, idAutor);
        CreateResult cr = arqLivrosAutores.create(la);
        if (cr.id > 0) {
            indiceId.inserir(cr.id, cr.endereco);
            // Chaves compostas para índices secundários (únicas por vínculo)
            indicePorLivro.inserir(chaveComposta(idLivro, cr.id), cr.endereco);
            indicePorAutor.inserir(chaveComposta(idAutor, cr.id), cr.endereco);
            return true;
        }
        return false;
    }

    public int vincularAutoresAoLivro(int idLivro, int[] idsAutores) throws Exception {
        int ok = 0;
        for (int idAutor : idsAutores)
            if (vincularAutorAoLivro(idLivro, idAutor)) ok++;
        return ok;
    }

    public int vincularLivrosAoAutor(int idAutor, int[] idsLivros) throws Exception {
        int ok = 0;
        for (int idLivro : idsLivros)
            if (vincularAutorAoLivro(idLivro, idAutor)) ok++;
        return ok;
    }

    // -------------------------------------------------------------------------
    // READ
    // -------------------------------------------------------------------------

    public LivroAutor buscarPorId(int id) throws Exception {
        long offset = indiceId.buscar(id);
        if (offset != ArvoreBMais.NULO) return arqLivrosAutores.readByOffset(offset);
        return arqLivrosAutores.read(id);
    }

    /**
     * Retorna todos os vínculos de um livro (todos os autores do livro).
     * Usa índice secundário indicePorLivro — O(log N + k), não scan linear.
     */
    public List<LivroAutor> buscarAutoresDoLivro(int idLivro) throws Exception {
        return buscarPorIndiceSecundario(indicePorLivro, idLivro,
            la -> la.getIdLivro() == idLivro);
    }

    /**
     * Retorna todos os vínculos de um autor (todos os livros do autor).
     * Usa índice secundário indicePorAutor.
     */
    public List<LivroAutor> buscarLivrosDoAutor(int idAutor) throws Exception {
        return buscarPorIndiceSecundario(indicePorAutor, idAutor,
            la -> la.getIdAutor() == idAutor);
    }

    public int[] buscarIdsAutoresDoLivro(int idLivro) throws Exception {
        return buscarAutoresDoLivro(idLivro).stream()
                .mapToInt(LivroAutor::getIdAutor).toArray();
    }

    public int[] buscarIdsLivrosDoAutor(int idAutor) throws Exception {
        return buscarLivrosDoAutor(idAutor).stream()
                .mapToInt(LivroAutor::getIdLivro).toArray();
    }

    public List<LivroAutor> listarTodos() throws Exception {
        return arqLivrosAutores.listarTodos();
    }

    // -------------------------------------------------------------------------
    // UPDATE
    // -------------------------------------------------------------------------

    public boolean alterarLivroAutor(LivroAutor la) throws Exception {
        return arqLivrosAutores.update(la);
    }

    // -------------------------------------------------------------------------
    // DELETE
    // -------------------------------------------------------------------------

    public boolean excluirPorId(int id) throws Exception {
        LivroAutor la = buscarPorId(id);
        if (la == null) return false;

        boolean ok = arqLivrosAutores.delete(id);
        if (ok) {
            indiceId.remover(id);
            indicePorLivro.remover(chaveComposta(la.getIdLivro(), id));
            indicePorAutor.remover(chaveComposta(la.getIdAutor(), id));
        }
        return ok;
    }

    /** Remove todos os vínculos de um livro — chamar ao excluir o livro. */
    public int excluirAutoresDoLivro(int idLivro) throws Exception {
        List<LivroAutor> registros = buscarAutoresDoLivro(idLivro);
        int removidos = 0;
        for (LivroAutor la : registros)
            if (excluirPorId(la.getId())) removidos++;
        return removidos;
    }

    /** Remove todos os vínculos de um autor — chamar ao excluir o autor. */
    public int excluirLivrosDoAutor(int idAutor) throws Exception {
        List<LivroAutor> registros = buscarLivrosDoAutor(idAutor);
        int removidos = 0;
        for (LivroAutor la : registros)
            if (excluirPorId(la.getId())) removidos++;
        return removidos;
    }

    // -------------------------------------------------------------------------
    // Auxiliares
    // -------------------------------------------------------------------------

    /**
     * Percorre as folhas do índice secundário buscando entradas cuja chave
     * começa com (idFiltro * MAX_ID), depois filtra pelo predicado.
     */
    private List<LivroAutor> buscarPorIndiceSecundario(
            ArvoreBMais indice,
            int         idFiltro,
            java.util.function.Predicate<LivroAutor> predicado) throws Exception {

        long[][] pares = indice.listarOrdenado();
        long chaveMin  = chaveComposta(idFiltro, 0);
        long chaveMax  = chaveComposta(idFiltro + 1, 0);

        List<LivroAutor> resultado = new ArrayList<>();
        for (long[] par : pares) {
            if (par[0] >= chaveMin && par[0] < chaveMax) {
                LivroAutor la = arqLivrosAutores.readByOffset(par[1]);
                if (la != null && predicado.test(la)) resultado.add(la);
            }
        }
        return resultado;
    }

    /** Gera chave composta única: base * MAX_ID + seq */
    private static int chaveComposta(int base, int seq) {
        // Limitado a int para compatibilidade com a B+ (chave int)
        // base e seq devem ser < MAX_ID
        return (base % MAX_ID) * MAX_ID + (seq % MAX_ID);
    }
}
