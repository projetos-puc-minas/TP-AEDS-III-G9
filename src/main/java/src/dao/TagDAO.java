package src.dao;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;

import src.model.Tag;
import src.util.Arquivo;
import src.util.Arquivo.CreateResult;
import src.util.ArvoreBMais;
import src.util.HashExtensivel;
import src.util.OrdenacaoExterna;


public class TagDAO {

    private final Arquivo<Tag>   arqTags;
    private final ArvoreBMais    indice;
    private final HashExtensivel hash;

    // Dependência injetada para verificação de integridade referencial (N:N)
    private TagsLivrosDAO tagsLivrosDAO;

    public TagDAO() throws Exception {
        arqTags = new Arquivo<>("tags", Tag.class.getConstructor());
        indice  = new ArvoreBMais("tags_id");
        hash    = new HashExtensivel("tags_id");
    }

    public void setTagsLivrosDAO(TagsLivrosDAO tagsLivrosDAO) {
        this.tagsLivrosDAO = tagsLivrosDAO;
    }

    // CREATE

    public int criarTag(Tag tag) throws Exception {
        CreateResult cr = arqTags.create(tag);
        if (cr.id > 0) {
            indice.inserir(cr.id, cr.endereco);
            hash.inserir(cr.id, cr.endereco);
        }
        return cr.id;
    }

    // READ

    public Tag buscarTag(int id) throws Exception {
        long offset = hash.buscar(id);
        if (offset != ArvoreBMais.NULO) {
            return arqTags.readByOffset(offset);
        }
        return arqTags.read(id);
    }

    public List<Tag> listarTodas() throws Exception {
        return arqTags.listarTodos();
    }

    // UPDATE

    public boolean alterarTag(Tag tag) throws Exception {
        boolean ok = arqTags.update(tag);
        if (ok) {
            reindexar(tag.getId());
        }
        return ok;
    }

    // DELETE 

    public boolean excluirTag(int id) throws Exception {
        if (tagsLivrosDAO != null && !tagsLivrosDAO.buscarLivrosDaTag(id).isEmpty()) {
            throw new IllegalStateException(
                "Não é possível excluir a Tag ID " + id +
                " pois existem livros vinculados a ela. " +
                "Use excluirTagEmCascata() ou remova os vínculos manualmente primeiro.");
        }

        boolean ok = arqTags.delete(id);
        if (ok) {
            indice.remover(id);
            hash.remover(id);
        }
        return ok;
    }


    public boolean excluirTagEmCascata(int id) throws Exception {
        if (tagsLivrosDAO != null) {
            tagsLivrosDAO.excluirLivrosDaTag(id);
        }
        boolean ok = arqTags.delete(id);
        if (ok) {
            indice.remover(id);
            hash.remover(id);
        }
        return ok;
    }

    // LISTAGEM ORDENADA

    public List<Tag> listarOrdenadoPorId() throws Exception {
        long[][] pares = indice.listarOrdenado();

        if (pares.length == 0 && arqTags.getTotalRegistros() > 0) {
            for (Arquivo.OffsetEntry<Tag> e : arqTags.listarComOffset()) {
                indice.inserir(e.objeto.getId(), e.offset);
            }
            pares = indice.listarOrdenado();
        }

        List<Tag> res = new ArrayList<>();
        for (long[] par : pares) {
            Tag t = arqTags.readByOffset(par[1]);
            if (t != null) res.add(t);
        }
        return res;
    }

    public List<Tag> listarOrdenadoDecrescentePorId() throws Exception {
        long[][] pares = indice.listarOrdenadoDecrescente();

        if (pares.length == 0 && arqTags.getTotalRegistros() > 0) {
            for (Arquivo.OffsetEntry<Tag> e : arqTags.listarComOffset()) {
                indice.inserir(e.objeto.getId(), e.offset);
            }
            pares = indice.listarOrdenadoDecrescente();
        }

        List<Tag> res = new ArrayList<>();
        for (long[] par : pares) {
            Tag t = arqTags.readByOffset(par[1]);
            if (t != null) res.add(t);
        }
        return res;
    }

    public List<Tag> listarOrdenadoPorNome() throws Exception {
        OrdenacaoExterna<Tag> ord = new OrdenacaoExterna<>(
            arqTags,
            Tag.class.getConstructor(),
            (a, b) -> a.getNome().compareToIgnoreCase(b.getNome())
        );
        long[][] pares = ord.ordenar();
        List<Tag> res = new ArrayList<>();
        for (long[] par : pares) {
            Tag t = arqTags.readByOffset(par[1]);
            if (t != null) res.add(t);
        }
        return res;
    }

    // Auxiliar 

    private void reindexar(int id) throws Exception {
        try (RandomAccessFile raf = new RandomAccessFile("./data/tags.bin", "r")) {
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