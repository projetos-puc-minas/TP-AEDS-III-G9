package src.dao;

import java.util.ArrayList;
import java.util.List;

import src.model.Livro;
import src.util.Arquivo;
import src.util.Arquivo.CreateResult;
import src.util.ArvoreBMais;

/**
 * DAO de Livros.
 *
 * Índice B+: chave = id, valor = offset físico no livros.bin.
 *
 * INTEGRIDADE REFERENCIAL:
 *   - excluirLivro() rejeita se houver vínculos em livros_autores.
 *   - EditoraDAO verifica livros vinculados via buscarLivrosPorEditora().
 *
 * CAMPO MULTIVALORADO:
 *   O campo generos (String[]) é serializado via SerializadorUtil.writeStringArray.
 */
public class LivroDAO {

    private final Arquivo<Livro> arqLivros;
    private final ArvoreBMais    indiceId;

    // Dependência injetada para verificação de integridade referencial
    private LivroAutorDAO livroAutorDAO;

    public LivroDAO() throws Exception {
        arqLivros = new Arquivo<>("livros", Livro.class.getConstructor());
        indiceId  = new ArvoreBMais("livros_id");
    }

    public void setLivroAutorDAO(LivroAutorDAO dao) { this.livroAutorDAO = dao; }

    // -------------------------------------------------------------------------
    // CREATE
    // -------------------------------------------------------------------------

    public boolean incluirLivro(Livro livro) throws Exception {
        CreateResult cr = arqLivros.create(livro);
        if (cr.id > 0) {
            indiceId.inserir(cr.id, cr.endereco);
            return true;
        }
        return false;
    }

    // -------------------------------------------------------------------------
    // READ
    // -------------------------------------------------------------------------

    public Livro buscarLivroPorId(int id) throws Exception {
        long offset = indiceId.buscar(id);
        if (offset != ArvoreBMais.NULO) {
            return arqLivros.readByOffset(offset);
        }
        return arqLivros.read(id);
    }

    public Livro buscarLivroPorIsbn(String isbn) throws Exception {
        for (Livro l : arqLivros.listarTodos()) {
            if (new String(l.getIsbn()).trim().equals(isbn.trim())) return l;
        }
        return null;
    }

    public Livro buscarLivroPorTitulo(String titulo) throws Exception {
        for (Livro l : arqLivros.listarTodos()) {
            if (l.getTitulo().equalsIgnoreCase(titulo)) return l;
        }
        return null;
    }

    public List<Livro> listarTodos() throws Exception {
        return arqLivros.listarTodos();
    }

    /**
     * Retorna todos os livros de uma editora (relacionamento 1:N).
     * Usado por EditoraDAO para verificação de integridade referencial.
     */
    public List<Livro> buscarLivrosPorEditora(int idEditora) throws Exception {
        List<Livro> resultado = new ArrayList<>();
        for (Livro l : arqLivros.listarTodos()) {
            if (l.getIdEditora() == idEditora) resultado.add(l);
        }
        return resultado;
    }

    // -------------------------------------------------------------------------
    // UPDATE
    // -------------------------------------------------------------------------

    public boolean alterarLivro(Livro livro) throws Exception {
        boolean ok = arqLivros.update(livro);
        if (ok) reindexar(livro.getId());
        return ok;
    }

    // -------------------------------------------------------------------------
    // DELETE — com integridade referencial
    // -------------------------------------------------------------------------

    /**
     * Exclui um livro.
     * Lança IllegalStateException se houver autores vinculados em livros_autores.
     * Para exclusão em cascata, use excluirLivroEmCascata().
     */
    public boolean excluirLivro(int id) throws Exception {
        verificarDependentes(id);
        boolean ok = arqLivros.delete(id);
        if (ok) indiceId.remover(id);
        return ok;
    }

    /**
     * Exclui o livro E todos os seus vínculos de autores.
     */
    public boolean excluirLivroEmCascata(int id) throws Exception {
        if (livroAutorDAO != null) livroAutorDAO.excluirAutoresDoLivro(id);
        boolean ok = arqLivros.delete(id);
        if (ok) indiceId.remover(id);
        return ok;
    }

    // -------------------------------------------------------------------------
    // LISTAGEM ORDENADA (travessia das folhas da B+)
    // -------------------------------------------------------------------------

    public List<Livro> listarOrdenadoPorId() throws Exception {
        long[][] pares  = indiceId.listarOrdenado();
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

    private void verificarDependentes(int id) throws Exception {
        if (livroAutorDAO != null && !livroAutorDAO.buscarAutoresDoLivro(id).isEmpty()) {
            throw new IllegalStateException(
                "Livro ID " + id + " possui autores vinculados. " +
                "Use excluirLivroEmCascata() ou remova os vínculos manualmente.");
        }
    }

    private void reindexar(int id) throws Exception {
        try (java.io.RandomAccessFile raf =
                new java.io.RandomAccessFile("./data/livros.bin", "r")) {
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
                    if (rid == id) { indiceId.atualizar(id, pos); return; }
                }
            }
        }
    }
}