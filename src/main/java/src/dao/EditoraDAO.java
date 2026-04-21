package src.dao;

import java.util.ArrayList;
import java.util.List;

import src.model.Editora;
import src.util.Arquivo;
import src.util.Arquivo.CreateResult;
import src.util.ArvoreBMais;

/**
 * DAO de Editoras.
 *
 * Índice B+: chave = id, valor = offset físico no editoras.bin.
 *
 * INTEGRIDADE REFERENCIAL:
 *   excluirEditora() rejeita a exclusão se houver livros vinculados
 *   (verificado via LivroDAO passado como dependência).
 */
public class EditoraDAO {

    private final Arquivo<Editora> arqEditoras;
    private final ArvoreBMais      indice;
    private LivroDAO               livroDAO; // injetado para verificar FK

    public EditoraDAO() throws Exception {
        arqEditoras = new Arquivo<>("editoras", Editora.class.getConstructor());
        indice      = new ArvoreBMais("editoras_id");
    }

    /** Injeta o LivroDAO para verificação de integridade referencial. */
    public void setLivroDAO(LivroDAO livroDAO) {
        this.livroDAO = livroDAO;
    }

    // -------------------------------------------------------------------------
    // CREATE
    // -------------------------------------------------------------------------

    public boolean incluirEditora(Editora editora) throws Exception {
        CreateResult cr = arqEditoras.create(editora);
        if (cr.id > 0) {
            indice.inserir(cr.id, cr.endereco); // offset real, não id
            return true;
        }
        return false;
    }

    // -------------------------------------------------------------------------
    // READ
    // -------------------------------------------------------------------------

    public Editora buscarEditoraPorId(int id) throws Exception {
        long offset = indice.buscar(id);
        if (offset != ArvoreBMais.NULO) {
            return arqEditoras.readByOffset(offset);
        }
        return arqEditoras.read(id); // fallback scan
    }

    public List<Editora> listarTodas() throws Exception {
        return arqEditoras.listarTodos();
    }

    // -------------------------------------------------------------------------
    // UPDATE
    // -------------------------------------------------------------------------

    public boolean alterarEditora(Editora editora) throws Exception {
        boolean ok = arqEditoras.update(editora);
        if (ok) {
            // Se o update moveu o registro (cresceu), o offset mudou.
            // Como Arquivo.update() não retorna o novo offset diretamente,
            // fazemos um scan para atualizar o índice.
            // (Melhoria futura: update() retornar o novo offset)
            arquivo_reindexarEditora(editora.getId());
        }
        return ok;
    }

    // -------------------------------------------------------------------------
    // DELETE — com verificação de integridade referencial
    // -------------------------------------------------------------------------

    /**
     * Exclui uma editora.
     * Lança IllegalStateException se houver livros vinculados a ela.
     */
    public boolean excluirEditora(int id) throws Exception {
        // Verificação de integridade referencial
        if (livroDAO != null) {
            List<?> livrosVinculados = livroDAO.buscarLivrosPorEditora(id);
            if (!livrosVinculados.isEmpty()) {
                throw new IllegalStateException(
                    "Não é possível excluir a editora ID " + id +
                    " pois há " + livrosVinculados.size() + " livro(s) vinculado(s) a ela.");
            }
        }

        boolean ok = arqEditoras.delete(id);
        if (ok) indice.remover(id);
        return ok;
    }

    // -------------------------------------------------------------------------
    // LISTAGEM ORDENADA por id (travessia das folhas da B+)
    // -------------------------------------------------------------------------

    public List<Editora> listarOrdenadoPorId() throws Exception {
        long[][] pares    = indice.listarOrdenado();
        List<Editora> res = new ArrayList<>();
        for (long[] par : pares) {
            Editora e = arqEditoras.readByOffset(par[1]); // par[1] = offset real
            if (e != null) res.add(e);
        }
        return res;
    }

    // -------------------------------------------------------------------------
    // Auxiliar — re-indexa uma editora após update que pode ter mudado o offset
    // -------------------------------------------------------------------------

    private void arquivo_reindexarEditora(int id) throws Exception {
        arqEditoras.listarTodos(); // força leitura sequencial
        // Scan para encontrar o novo offset
        // (simplificado — ideal seria Arquivo.update() retornar o novo offset)
        java.io.RandomAccessFile raf = null;
        try {
            raf = new java.io.RandomAccessFile("./data/editoras.bin", "r");
            raf.seek(Arquivo.TAM_CABECALHO);
            while (raf.getFilePointer() < raf.length()) {
                long   pos    = raf.getFilePointer();
                boolean lapide = raf.readBoolean();
                int    tam    = raf.readInt();
                byte[] dados  = new byte[tam];
                raf.readFully(dados);
                if (lapide) {
                    java.io.DataInputStream dis =
                        new java.io.DataInputStream(
                            new java.io.ByteArrayInputStream(dados));
                    int rid = dis.readInt();
                    if (rid == id) {
                        indice.atualizar(id, pos);
                        return;
                    }
                }
            }
        } finally {
            if (raf != null) raf.close();
        }
    }
}
