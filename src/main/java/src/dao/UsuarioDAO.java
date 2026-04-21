package src.dao;

import java.util.List;

import src.model.Usuarios;
import src.util.Arquivo;
import src.util.Arquivo.CreateResult;
import src.util.ArvoreBMais;

/**
 * DAO de Usuários.
 *
 * Índice B+: chave = id, valor = offset físico no usuarios.bin.
 */
public class UsuarioDAO {

    private final Arquivo<Usuarios> arqUsuarios;
    private final ArvoreBMais       indice;

    public UsuarioDAO() throws Exception {
        arqUsuarios = new Arquivo<>("usuarios", Usuarios.class.getConstructor());
        indice      = new ArvoreBMais("usuarios_id");
    }

    // -------------------------------------------------------------------------
    // CREATE
    // -------------------------------------------------------------------------

    public int incluirUsuario(Usuarios usuario) throws Exception {
        CreateResult cr = arqUsuarios.create(usuario);
        if (cr.id > 0) indice.inserir(cr.id, cr.endereco);
        return cr.id;
    }

    // -------------------------------------------------------------------------
    // READ
    // -------------------------------------------------------------------------

    public Usuarios buscarUsuario(int id) throws Exception {
        long offset = indice.buscar(id);
        if (offset != ArvoreBMais.NULO) return arqUsuarios.readByOffset(offset);
        return arqUsuarios.read(id);
    }

    /**
     * Busca usuário pelo email — necessário para o fluxo de login.
     * Scan linear (email sem índice dedicado).
     */
    public Usuarios buscarPorEmail(String email) throws Exception {
        for (Usuarios u : arqUsuarios.listarTodos()) {
            if (u.getEmail().equalsIgnoreCase(email)) return u;
        }
        return null;
    }

    public List<Usuarios> listarTodos() throws Exception {
        return arqUsuarios.listarTodos();
    }

    // -------------------------------------------------------------------------
    // UPDATE
    // -------------------------------------------------------------------------

    public boolean alterarUsuario(Usuarios usuario) throws Exception {
        boolean ok = arqUsuarios.update(usuario);
        if (ok) reindexar(usuario.getId());
        return ok;
    }

    // -------------------------------------------------------------------------
    // DELETE
    // -------------------------------------------------------------------------

    public boolean excluirUsuario(int id) throws Exception {
        boolean ok = arqUsuarios.delete(id);
        if (ok) indice.remover(id);
        return ok;
    }

    // -------------------------------------------------------------------------
    // Auxiliar
    // -------------------------------------------------------------------------

    private void reindexar(int id) throws Exception {
        try (java.io.RandomAccessFile raf =
                new java.io.RandomAccessFile("./data/usuarios.bin", "r")) {
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