package src.dao;

import java.util.ArrayList;
import java.util.List;

import src.model.Autores;
import src.util.Arquivo;
import src.util.Arquivo.CreateResult;
import src.util.ArvoreBMais;

/**
 * DAO de Autores.
 *
 * Índice B+: chave = id, valor = offset físico no autores.bin.
 *
 * INTEGRIDADE REFERENCIAL:
 *   excluirAutor() rejeita a exclusão se houver vínculos na tabela
 *   livros_autores (verificado via LivroAutorDAO).
 */
public class AutoresDAO {

    private final Arquivo<Autores> arqAutores;
    private final ArvoreBMais      indice;
    private LivroAutorDAO          livroAutorDAO; // injetado

    public AutoresDAO() throws Exception {
        arqAutores = new Arquivo<>("autores", Autores.class.getConstructor());
        indice     = new ArvoreBMais("autores_id");
    }

    public void setLivroAutorDAO(LivroAutorDAO livroAutorDAO) {
        this.livroAutorDAO = livroAutorDAO;
    }

    // -------------------------------------------------------------------------
    // CREATE
    // -------------------------------------------------------------------------

    public int adicionarAutor(Autores autor) throws Exception {
        CreateResult cr = arqAutores.create(autor);
        if (cr.id > 0) {
            indice.inserir(cr.id, cr.endereco);
        }
        return cr.id;
    }

    // -------------------------------------------------------------------------
    // READ
    // -------------------------------------------------------------------------

    public Autores buscarAutor(int id) throws Exception {
        long offset = indice.buscar(id);
        if (offset != ArvoreBMais.NULO) {
            return arqAutores.readByOffset(offset);
        }
        return arqAutores.read(id);
    }

    public List<Autores> listarTodos() throws Exception {
        return arqAutores.listarTodos();
    }

    // -------------------------------------------------------------------------
    // UPDATE
    // -------------------------------------------------------------------------

    public boolean alterarAutor(Autores autor) throws Exception {
        boolean ok = arqAutores.update(autor);
        if (ok) reindexar(autor.getId());
        return ok;
    }

    // -------------------------------------------------------------------------
    // DELETE — com integridade referencial
    // -------------------------------------------------------------------------

    /**
     * Exclui um autor.
     * Lança IllegalStateException se houver livros vinculados a ele.
     */
    public boolean excluirAutor(int id) throws Exception {
        if (livroAutorDAO != null) {
            List<?> vinculos = livroAutorDAO.buscarAutoresDoLivro(id);
            // buscarLivrosDoAutor é o método correto aqui
            List<?> livros = livroAutorDAO.buscarLivrosDoAutor(id);
            if (!livros.isEmpty()) {
                throw new IllegalStateException(
                    "Não é possível excluir o autor ID " + id +
                    " pois há " + livros.size() + " livro(s) vinculado(s). " +
                    "Remova os vínculos antes de excluir o autor.");
            }
        }
        boolean ok = arqAutores.delete(id);
        if (ok) indice.remover(id);
        return ok;
    }

    // -------------------------------------------------------------------------
    // LISTAGEM ORDENADA
    // -------------------------------------------------------------------------

    public List<Autores> listarOrdenadoPorId() throws Exception {
        long[][] pares   = indice.listarOrdenado();
        List<Autores> res = new ArrayList<>();
        for (long[] par : pares) {
            Autores a = arqAutores.readByOffset(par[1]);
            if (a != null) res.add(a);
        }
        return res;
    }

    // -------------------------------------------------------------------------
    // Auxiliar
    // -------------------------------------------------------------------------

    private void reindexar(int id) throws Exception {
        try (java.io.RandomAccessFile raf =
                new java.io.RandomAccessFile("./data/autores.bin", "r")) {
            raf.seek(Arquivo.TAM_CABECALHO);
            while (raf.getFilePointer() < raf.length()) {
                long    pos    = raf.getFilePointer();
                boolean lapide = raf.readBoolean();
                int     tam    = raf.readInt();
                byte[]  dados  = new byte[tam];
                raf.readFully(dados);
                if (lapide) {
                    java.io.DataInputStream dis =
                        new java.io.DataInputStream(
                            new java.io.ByteArrayInputStream(dados));
                    int rid = dis.readInt();
                    if (rid == id) { indice.atualizar(id, pos); return; }
                }
            }
        }
    }
}
