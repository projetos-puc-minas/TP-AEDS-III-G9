package src.dao;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;

import src.model.Editora;
import src.util.Arquivo;
import src.util.Arquivo.CreateResult;
import src.util.ArvoreBMais;
import src.util.HashExtensivel;
import src.util.OrdenacaoExterna;

/**
 * DAO de Editoras.
 *
 * * Índice B+: chave = id, valor = offset físico no editoras.bin.
 *
 * INTEGRIDADE REFERENCIAL (1:N):
 * - O método excluirEditora() verifica se existem livros dependentes
 * antes de proceder à remoção física, garantindo que não fiquem
 * registos "órfãos" no sistema.
 */
public class EditoraDAO {

    private final Arquivo<Editora> arqEditoras;
    private final ArvoreBMais      indice;   // B+: listagem ordenada
    private final HashExtensivel   hash;     // Hash: busca direta
    private LivroDAO               livroDAO; // Dependência para verificar integridade

    public EditoraDAO() throws Exception {
        arqEditoras = new Arquivo<>("editoras", Editora.class.getConstructor());
        indice      = new ArvoreBMais("editoras_id");
        hash        = new HashExtensivel("editoras_id");
    }

    /** Injeta o LivroDAO para permitir a verificação de chaves estrangeiras. */
    public void setLivroDAO(LivroDAO livroDAO) {
        this.livroDAO = livroDAO;
    }

    // -------------------------------------------------------------------------
    // CREATE
    // -------------------------------------------------------------------------

    public boolean incluirEditora(Editora editora) throws Exception {
        CreateResult cr = arqEditoras.create(editora);
        if (cr.id > 0) {
            indice.inserir(cr.id, cr.endereco);
            hash.inserir(cr.id, cr.endereco);
            return true;
        }
        return false;
    }

    // -------------------------------------------------------------------------
    // READ
    // -------------------------------------------------------------------------

    public Editora buscarEditora(int id) throws Exception {
        long offset = hash.buscar(id); // Hash: O(1) amortizado
        if (offset != ArvoreBMais.NULO) {
            return arqEditoras.readByOffset(offset);
        }
        return arqEditoras.read(id);
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
            reindexar(editora.getId());
        }
        return ok;
    }

    // -------------------------------------------------------------------------
    // DELETE (com validação de integridade)
    // -------------------------------------------------------------------------

    /**
     * Exclui uma editora apenas se não houver livros vinculados a ela.
     * @throws IllegalStateException caso existam livros dependentes.
     */
    public boolean excluirEditora(int id) throws Exception {
        // Verifica se existem livros desta editora no LivroDAO
        if (livroDAO != null && !livroDAO.buscarLivrosPorEditora(id).isEmpty()) {
            throw new IllegalStateException(
                "Não é possível excluir a Editora ID " + id + 
                " pois existem livros vinculados a ela.");
        }

        boolean ok = arqEditoras.delete(id);
        if (ok) {
            indice.remover(id);
            hash.remover(id);
        }
        return ok;
    }

    // -------------------------------------------------------------------------
    // LISTAGEM ORDENADA (via B+)
    // -------------------------------------------------------------------------

    public List<Editora> listarOrdenadoPorId() throws Exception {
        long[][] pares = indice.listarOrdenado();
        if (pares.length == 0 && arqEditoras.getTotalRegistros() > 0) {
            for (Arquivo.OffsetEntry<Editora> e : arqEditoras.listarComOffset()) {
                indice.inserir(e.objeto.getId(), e.offset);
            }
            pares = indice.listarOrdenado();
        }
        List<Editora> res = new ArrayList<>();
        for (long[] par : pares) {
            Editora e = arqEditoras.readByOffset(par[1]);
            if (e != null) res.add(e);
        }
        return res;
    }

    public List<Editora> listarOrdenadoDecrescentePorId() throws Exception {
        long[][] pares = indice.listarOrdenadoDecrescente();
        if (pares.length == 0 && arqEditoras.getTotalRegistros() > 0) {
            for (Arquivo.OffsetEntry<Editora> e : arqEditoras.listarComOffset()) {
                indice.inserir(e.objeto.getId(), e.offset);
            }
            pares = indice.listarOrdenadoDecrescente();
        }
        List<Editora> res = new ArrayList<>();
        for (long[] par : pares) {
            Editora e = arqEditoras.readByOffset(par[1]);
            if (e != null) res.add(e);
        }
        return res;
    }

    /**
     * Ordenação externa por intercalação — ordena editoras pelo nome
     * sem carregar todos os registros na RAM de uma vez.
     */
    public List<Editora> listarOrdenadoPorNome() throws Exception {
        OrdenacaoExterna<Editora> ord = new OrdenacaoExterna<>(
            arqEditoras,
            Editora.class.getConstructor(),
            (a, b) -> a.getNome().compareToIgnoreCase(b.getNome())
        );
        long[][] pares = ord.ordenar();
        List<Editora> res = new ArrayList<>();
        for (long[] par : pares) {
            Editora e = arqEditoras.readByOffset(par[1]);
            if (e != null) res.add(e);
        }
        return res;
    }

    // -------------------------------------------------------------------------
    // Auxiliar (Otimizado)
    // -------------------------------------------------------------------------

    /**
     * Localiza o novo offset de um registo após um update estrutural.
     * Salta blocos apagados para poupar recursos.
     */
    private void reindexar(int id) throws Exception {
        try (RandomAccessFile raf = new RandomAccessFile("./data/editoras.bin", "r")) {
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
                        indice.atualizar(id, pos);
                        hash.atualizar(id, pos);
                        return;
                    }
                } else {
                    raf.skipBytes(tamFisico);
                }
            }
        }
    }
}