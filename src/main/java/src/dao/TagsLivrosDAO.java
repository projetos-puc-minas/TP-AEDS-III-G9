package src.dao;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import src.model.TagsLivros;
import src.util.Arquivo;
import src.util.Arquivo.CreateResult;
import src.util.ArvoreBMais;
import src.util.HashExtensivel;
import src.util.OrdenacaoExterna;


public class TagsLivrosDAO {

    private static final int MAX_ID = 100_000;

    private final Arquivo<TagsLivros> arqTagsLivros;
    private final ArvoreBMais         indiceId;
    private final HashExtensivel      hashId;
    private final ArvoreBMais         indicePorTag;
    private final ArvoreBMais         indicePorLivro;

    public TagsLivrosDAO() throws Exception {
        arqTagsLivros  = new Arquivo<>("tags_livros", TagsLivros.class.getConstructor());
        indiceId       = new ArvoreBMais("tags_livros_id");
        hashId         = new HashExtensivel("tags_livros_id");
        indicePorTag   = new ArvoreBMais("tags_livros_por_tag");
        indicePorLivro = new ArvoreBMais("tags_livros_por_livro");
    }

    // CREATE 


    public boolean vincularTagAoLivro(int idTag, int idLivro) throws Exception {
        TagsLivros novo = new TagsLivros(idTag, idLivro);
        CreateResult cr = arqTagsLivros.create(novo);

        if (cr.id > 0) {
            indiceId.inserir(cr.id, cr.endereco);
            hashId.inserir(cr.id, cr.endereco);
            indicePorTag.inserir((int) chaveComposta(idTag, cr.id), cr.endereco);
            indicePorLivro.inserir((int) chaveComposta(idLivro, cr.id), cr.endereco);
            return true;
        }
        return false;
    }

    // READ

    public TagsLivros buscarPorId(int id) throws Exception {
        long offset = hashId.buscar(id);
        if (offset != ArvoreBMais.NULO) {
            return arqTagsLivros.readByOffset(offset);
        }
        return arqTagsLivros.read(id);
    }

    public List<TagsLivros> listarTodos() throws Exception {
        return arqTagsLivros.listarTodos();
    }

    /** Busca todos os livros vinculados a uma tag via Range Scan na B+. */
    public List<TagsLivros> buscarLivrosDaTag(int idTag) throws Exception {
        return buscarPorIndiceSecundario(
            indicePorTag,
            idTag,
            tl -> tl.getIdTag() == idTag
        );
    }

    /** Busca todas as tags vinculadas a um livro via Range Scan na B+. */
    public List<TagsLivros> buscarTagsDoLivro(int idLivro) throws Exception {
        return buscarPorIndiceSecundario(
            indicePorLivro,
            idLivro,
            tl -> tl.getIdLivro() == idLivro
        );
    }

    // DELETE


    public boolean excluirPorId(int id) throws Exception {
        TagsLivros tl = buscarPorId(id);
        if (tl == null) return false;

        boolean ok = arqTagsLivros.delete(id);
        if (ok) {
            indiceId.remover(id);
            hashId.remover(id);
            indicePorTag.remover((int) chaveComposta(tl.getIdTag(), id));
            indicePorLivro.remover((int) chaveComposta(tl.getIdLivro(), id));
        }
        return ok;
    }

    /** Remove todos os vínculos de um livro (Delete em Cascata a partir do Livro). */
    public int excluirTagsDoLivro(int idLivro) throws Exception {
        List<TagsLivros> registros = buscarTagsDoLivro(idLivro);
        int removidos = 0;
        for (TagsLivros tl : registros) {
            if (excluirPorId(tl.getId())) removidos++;
        }
        return removidos;
    }

    /** Remove todos os vínculos de uma tag (Delete em Cascata a partir da Tag). */
    public int excluirLivrosDaTag(int idTag) throws Exception {
        List<TagsLivros> registros = buscarLivrosDaTag(idTag);
        int removidos = 0;
        for (TagsLivros tl : registros) {
            if (excluirPorId(tl.getId())) removidos++;
        }
        return removidos;
    }

    public List<TagsLivros> listarOrdenadoPorId() throws Exception {
        long[][] pares = indiceId.listarOrdenado();
        List<TagsLivros> res = new ArrayList<>();
        for (long[] par : pares) {
            TagsLivros tl = arqTagsLivros.readByOffset(par[1]);
            if (tl != null) res.add(tl);
        }
        return res;
    }

    public List<TagsLivros> listarOrdenadoDecrescentePorId() throws Exception {
        long[][] pares = indiceId.listarOrdenadoDecrescente();
        List<TagsLivros> res = new ArrayList<>();
        for (long[] par : pares) {
            TagsLivros tl = arqTagsLivros.readByOffset(par[1]);
            if (tl != null) res.add(tl);
        }
        return res;
    }


    public List<TagsLivros> listarOrdenadoPorIdTag() throws Exception {
        OrdenacaoExterna<TagsLivros> ord = new OrdenacaoExterna<>(
            arqTagsLivros,
            TagsLivros.class.getConstructor(),
            (a, b) -> Integer.compare(a.getIdTag(), b.getIdTag())
        );
        long[][] pares = ord.ordenar();
        List<TagsLivros> res = new ArrayList<>();
        for (long[] par : pares) {
            TagsLivros tl = arqTagsLivros.readByOffset(par[1]);
            if (tl != null) res.add(tl);
        }
        return res;
    }

    // Auxiliares 

    private List<TagsLivros> buscarPorIndiceSecundario(
            ArvoreBMais indice,
            int         idFiltro,
            Predicate<TagsLivros> predicado) throws Exception {

        long[][] pares   = indice.listarOrdenado();
        long chaveMin    = chaveComposta(idFiltro, 0);
        long chaveMax    = chaveComposta(idFiltro + 1, 0);

        List<TagsLivros> resultado = new ArrayList<>();
        for (long[] par : pares) {
            if (par[0] >= chaveMin && par[0] < chaveMax) {
                TagsLivros tl = arqTagsLivros.readByOffset(par[1]);
                if (tl != null && predicado.test(tl)) {
                    resultado.add(tl);
                }
            }
        }
        return resultado;
    }

    private static long chaveComposta(int base, int seq) {
        return ((long) base * MAX_ID) + seq;
    }

    public void reindexar(int id) throws Exception {
        TagsLivros antigo = buscarPorId(id);
        if (antigo == null) return;

        try (RandomAccessFile raf = new RandomAccessFile("./data/tags_livros.bin", "r")) {
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
                        indicePorTag.atualizar((int) chaveComposta(antigo.getIdTag(), id), pos);
                        indicePorLivro.atualizar((int) chaveComposta(antigo.getIdLivro(), id), pos);
                        return;
                    }
                } else {
                    raf.skipBytes(tamFisico);
                }
            }
        }
    }
}