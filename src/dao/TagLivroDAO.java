package src.dao;

import java.util.List;
import java.util.stream.Collectors;
import src.model.TagLivro;
import src.util.Arquivo;
import src.util.Indexador;

public class TagLivroDAO {

    private final Arquivo<TagLivro> arqTagsLivros;
    private final Indexador indice;

    public TagLivroDAO(Indexador indice) throws Exception {
        this.arqTagsLivros = new Arquivo<>("tags_livros", TagLivro.class.getConstructor());
        this.indice        = indice;
    }

    // --- create ---

    public boolean vincularTagAoLivro(int idLivro, int idTag) throws Exception {
        TagLivro tl = new TagLivro(idLivro, idTag);
        int id = arqTagsLivros.create(tl, indice);
        return id > 0;
    }

    // vincula múltiplas tags a um mesmo livro (N:N — um livro tem muitas tags)
    public int vincularTagsAoLivro(int idLivro, int[] idsTags) throws Exception {
        int vinculados = 0;

        for (int idTag : idsTags) {
            if (vincularTagAoLivro(idLivro, idTag)) {
                vinculados++;
            }
        }

        return vinculados;
    }

    // vincula múltiplos livros a uma mesma tag (N:N — uma tag pertence a muitos livros)
    public int vincularLivrosATag(int idTag, int[] idsLivros) throws Exception {
        int vinculados = 0;

        for (int idLivro : idsLivros) {
            if (vincularTagAoLivro(idLivro, idTag)) {
                vinculados++;
            }
        }

        return vinculados;
    }

    // --- read ---

    public TagLivro buscarPorId(int id) throws Exception {
        if (indice.buscar(id) < 0) {
            return null;
        }
        return arqTagsLivros.read(id, indice);
    }

    // retorna todos os vínculos de um livro (todas as tags do livro)
    public List<TagLivro> buscarTagsDoLivro(int idLivro) throws Exception {
        return arqTagsLivros.listarTodos()
                .stream()
                .filter(tl -> tl.getIdLivro() == idLivro)
                .collect(Collectors.toList());
    }

    // retorna todos os vínculos de uma tag (todos os livros com essa tag)
    public List<TagLivro> buscarLivrosDaTag(int idTag) throws Exception {
        return arqTagsLivros.listarTodos()
                .stream()
                .filter(tl -> tl.getIdTag() == idTag)
                .collect(Collectors.toList());
    }

    // retorna apenas os IDs das tags de um livro
    public int[] buscarIdsTagsDoLivro(int idLivro) throws Exception {
        return buscarTagsDoLivro(idLivro)
                .stream()
                .mapToInt(TagLivro::getIdTag)
                .toArray();
    }

    // retorna apenas os IDs dos livros de uma tag
    public int[] buscarIdsLivrosDaTag(int idTag) throws Exception {
        return buscarLivrosDaTag(idTag)
                .stream()
                .mapToInt(TagLivro::getIdLivro)
                .toArray();
    }

    public List<TagLivro> listarTodos() throws Exception {
        return arqTagsLivros.listarTodos();
    }

    // --- update ---

    public boolean alterarTagLivro(TagLivro tagLivro) throws Exception {
        return arqTagsLivros.update(tagLivro, indice);
    }

    // --- delete ---

    public boolean excluirPorId(int id) throws Exception {
        return arqTagsLivros.delete(id, indice);
    }

    // remove todos os vínculos de um livro (ex: ao excluir o livro)
    public int excluirTagsDoLivro(int idLivro) throws Exception {
        List<TagLivro> registros = buscarTagsDoLivro(idLivro);

        int removidos = 0;

        for (TagLivro tl : registros) {
            if (excluirPorId(tl.getId())) {
                removidos++;
            }
        }

        return removidos;
    }

    // remove todos os vínculos de uma tag (ex: ao excluir a tag)
    public int excluirLivrosDaTag(int idTag) throws Exception {
        List<TagLivro> registros = buscarLivrosDaTag(idTag);

        int removidos = 0;

        for (TagLivro tl : registros) {
            if (excluirPorId(tl.getId())) {
                removidos++;
            }
        }

        return removidos;
    }
}