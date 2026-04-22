package src.dao;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;

import src.model.Autores;
import src.util.Arquivo;
import src.util.Arquivo.CreateResult;
import src.util.ArvoreBMais;
import src.util.HashExtensivel;
import src.util.OrdenacaoExterna;

/**
 * DAO de Autores.
 *
 * * Índice B+: chave = id, valor = offset físico no autores.bin.
 *
 * INTEGRIDADE REFERENCIAL:
 * - O método excluirAutor() rejeita a exclusão se houver vínculos na
 * tabela intermédia livros_autores (verificado via LivroAutorDAO).
 */
public class AutoresDAO {

    private final Arquivo<Autores> arqAutores;
    private final ArvoreBMais      indice;   // B+: listagem ordenada
    private final HashExtensivel   hash;     // Hash: busca direta
    
    // Dependência injetada para garantir a integridade do N:N
    private LivroAutorDAO livroAutorDAO;

    public AutoresDAO() throws Exception {
        arqAutores = new Arquivo<>("autores", Autores.class.getConstructor());
        indice     = new ArvoreBMais("autores_id");
        hash       = new HashExtensivel("autores_id");
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
            hash.inserir(cr.id, cr.endereco);
        }
        return cr.id;
    }

    // -------------------------------------------------------------------------
    // READ
    // -------------------------------------------------------------------------

    public Autores buscarAutor(int id) throws Exception {
        long offset = hash.buscar(id); // Hash: O(1) amortizado
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
        if (ok) {
            reindexar(autor.getId());
        }
        return ok;
    }

    // -------------------------------------------------------------------------
    // DELETE (com Integridade Referencial)
    // -------------------------------------------------------------------------

    /**
     * Exclui um autor de forma lógica e atualiza o índice.
     * Lança IllegalStateException se o autor possuir livros vinculados.
     */
    public boolean excluirAutor(int id) throws Exception {
        if (livroAutorDAO != null && !livroAutorDAO.buscarLivrosDoAutor(id).isEmpty()) {
            throw new IllegalStateException(
                "Não é possível excluir o Autor ID " + id + 
                " pois existem livros vinculados a ele na tabela livros_autores.");
        }

        boolean ok = arqAutores.delete(id);
        if (ok) {
            indice.remover(id);
            hash.remover(id);
        }
        return ok;
    }

    // -------------------------------------------------------------------------
    // LISTAGEM ORDENADA (via B+)
    // -------------------------------------------------------------------------

    public List<Autores> listarOrdenadoPorId() throws Exception {
        long[][] pares = indice.listarOrdenado();
        if (pares.length == 0 && arqAutores.getTotalRegistros() > 0) {
            for (Arquivo.OffsetEntry<Autores> e : arqAutores.listarComOffset()) {
                indice.inserir(e.objeto.getId(), e.offset);
            }
            pares = indice.listarOrdenado();
        }
        List<Autores> res = new ArrayList<>();
        for (long[] par : pares) {
            Autores a = arqAutores.readByOffset(par[1]);
            if (a != null) res.add(a);
        }
        return res;
    }

    public List<Autores> listarOrdenadoDecrescentePorId() throws Exception {
        long[][] pares = indice.listarOrdenadoDecrescente();
        if (pares.length == 0 && arqAutores.getTotalRegistros() > 0) {
            for (Arquivo.OffsetEntry<Autores> e : arqAutores.listarComOffset()) {
                indice.inserir(e.objeto.getId(), e.offset);
            }
            pares = indice.listarOrdenadoDecrescente();
        }
        List<Autores> res = new ArrayList<>();
        for (long[] par : pares) {
            Autores a = arqAutores.readByOffset(par[1]);
            if (a != null) res.add(a);
        }
        return res;
    }

    /**
     * Ordenação externa por intercalação — ordena autores pelo nome
     * sem carregar todos os registros na RAM de uma vez.
     */
    public List<Autores> listarOrdenadoPorNome() throws Exception {
        OrdenacaoExterna<Autores> ord = new OrdenacaoExterna<>(
            arqAutores,
            Autores.class.getConstructor(),
            (a, b) -> a.getNome().compareToIgnoreCase(b.getNome())
        );
        long[][] pares = ord.ordenar();
        List<Autores> res = new ArrayList<>();
        for (long[] par : pares) {
            Autores a = arqAutores.readByOffset(par[1]);
            if (a != null) res.add(a);
        }
        return res;
    }

    // -------------------------------------------------------------------------
    // Auxiliar (Otimizado)
    // -------------------------------------------------------------------------

    /**
     * Varre o ficheiro para encontrar o novo offset físico após um update.
     * Salta blocos excluídos (lápide falsa) sem os carregar para a memória RAM.
     */
    private void reindexar(int id) throws Exception {
        try (RandomAccessFile raf = new RandomAccessFile("./data/autores.bin", "r")) {
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
                    raf.skipBytes(tamFisico); // Otimização de I/O e RAM
                }
            }
        }
    }
}