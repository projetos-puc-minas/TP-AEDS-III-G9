package src.dao;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;

import src.model.Usuarios;
import src.util.Arquivo;
import src.util.Arquivo.CreateResult;
import src.util.ArvoreBMais;
import src.util.HashExtensivel;
import src.util.OrdenacaoExterna;

/**
 * DAO de Utilizadores.
 *
 * * Índice Primário (B+): chave = id, valor = offset físico no usuarios.bin.
 * * A busca por email (necessária para o login) é feita via varrimento
 * sequencial (linear scan), uma vez que não existe índice secundário
 * dedicado a este campo textual.
 */
public class UsuarioDAO {

    private final Arquivo<Usuarios> arqUsuarios;
    private final ArvoreBMais       indice;   // B+: listagem ordenada
    private final HashExtensivel    hash;     // Hash: busca direta

    public UsuarioDAO() throws Exception {
        arqUsuarios = new Arquivo<>("usuarios", Usuarios.class.getConstructor());
        indice      = new ArvoreBMais("usuarios_id");
        hash        = new HashExtensivel("usuarios_id");
    }

    // -------------------------------------------------------------------------
    // CREATE
    // -------------------------------------------------------------------------

    public int incluirUsuario(Usuarios usuario) throws Exception {
        CreateResult cr = arqUsuarios.create(usuario);
        if (cr.id > 0) {
            indice.inserir(cr.id, cr.endereco);
            hash.inserir(cr.id, cr.endereco);
        }
        return cr.id;
    }

    // -------------------------------------------------------------------------
    // READ
    // -------------------------------------------------------------------------

    public Usuarios buscarUsuario(int id) throws Exception {
        long offset = hash.buscar(id); // Hash: O(1) amortizado
        if (offset != ArvoreBMais.NULO) {
            return arqUsuarios.readByOffset(offset);
        }
        return arqUsuarios.read(id); // Fallback de segurança (scan)
    }

    /**
     * Busca um utilizador pelo email — essencial para o fluxo de login.
     * Como não temos um Hash ou B+ para o email, fazemos um scan sequencial.
     */
    public Usuarios buscarPorEmail(String email) throws Exception {
        for (Usuarios u : arqUsuarios.listarTodos()) {
            if (u.getEmail().equalsIgnoreCase(email.trim())) {
                return u;
            }
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
        if (ok) {
            // Se o update precisou de mover o registo de sítio (não coube no bloco),
            // o offset mudou. Temos de atualizar o índice B+.
            reindexar(usuario.getId());
        }
        return ok;
    }

    // -------------------------------------------------------------------------
    // DELETE
    // -------------------------------------------------------------------------

    public boolean excluirUsuario(int id) throws Exception {
        boolean ok = arqUsuarios.delete(id);
        if (ok) {
            indice.remover(id);
            hash.remover(id);
        }
        return ok;
    }

    // -------------------------------------------------------------------------
    // LISTAGEM ORDENADA (via B+ e OrdenacaoExterna)
    // -------------------------------------------------------------------------

    public List<Usuarios> listarOrdenadoPorId() throws Exception {
        long[][] pares = indice.listarOrdenado();
        if (pares.length == 0 && arqUsuarios.getTotalRegistros() > 0) {
            for (Arquivo.OffsetEntry<Usuarios> e : arqUsuarios.listarComOffset()) {
                indice.inserir(e.objeto.getId(), e.offset);
            }
            pares = indice.listarOrdenado();
        }
        List<Usuarios> res = new ArrayList<>();
        for (long[] par : pares) {
            Usuarios u = arqUsuarios.readByOffset(par[1]);
            if (u != null) res.add(u);
        }
        return res;
    }

    public List<Usuarios> listarOrdenadoDecrescentePorId() throws Exception {
        long[][] pares = indice.listarOrdenadoDecrescente();
        if (pares.length == 0 && arqUsuarios.getTotalRegistros() > 0) {
            for (Arquivo.OffsetEntry<Usuarios> e : arqUsuarios.listarComOffset()) {
                indice.inserir(e.objeto.getId(), e.offset);
            }
            pares = indice.listarOrdenadoDecrescente();
        }
        List<Usuarios> res = new ArrayList<>();
        for (long[] par : pares) {
            Usuarios u = arqUsuarios.readByOffset(par[1]);
            if (u != null) res.add(u);
        }
        return res;
    }

    /**
     * Ordenação externa por intercalação — ordena usuários pelo nome
     * sem carregar todos os registros na RAM de uma vez.
     */
    public List<Usuarios> listarOrdenadoPorNome() throws Exception {
        OrdenacaoExterna<Usuarios> ord = new OrdenacaoExterna<>(
            arqUsuarios,
            Usuarios.class.getConstructor(),
            (a, b) -> a.getNome().compareToIgnoreCase(b.getNome())
        );
        long[][] pares = ord.ordenar();
        List<Usuarios> res = new ArrayList<>();
        for (long[] par : pares) {
            Usuarios u = arqUsuarios.readByOffset(par[1]);
            if (u != null) res.add(u);
        }
        return res;
    }

    // -------------------------------------------------------------------------
    // Auxiliar (Reindexação Rápida)
    // -------------------------------------------------------------------------

    /**
     * Varre o ficheiro binário para encontrar o novo offset físico de um registo
     * e atualiza o índice B+. Utilizado após um update estrutural.
     */
    private void reindexar(int id) throws Exception {
        try (RandomAccessFile raf = new RandomAccessFile("./data/usuarios.bin", "r")) {
            raf.seek(Arquivo.TAM_CABECALHO);
            
            while (raf.getFilePointer() < raf.length()) {
                long    pos       = raf.getFilePointer();
                boolean lapide    = raf.readBoolean();
                int     tamFisico = raf.readInt();
                
                if (lapide) {
                    byte[] dados = new byte[tamFisico];
                    raf.readFully(dados);
                    
                    // O 'id' é sempre o primeiro int serializado no toByteArray().
                    // Lemos apenas o início do array para descobrir se é o alvo.
                    ByteArrayInputStream bais = new ByteArrayInputStream(dados);
                    DataInputStream      dis  = new DataInputStream(bais);
                    
                    int idLido = dis.readInt();
                    
                    if (idLido == id) {
                        indice.atualizar(id, pos);
                        hash.atualizar(id, pos);
                        return;
                    }
                } else {
                    // Otimização: se o registo estiver excluído, saltamos os bytes
                    // sem os carregar para a memória RAM.
                    raf.skipBytes(tamFisico);
                }
            }
        }
    }
}