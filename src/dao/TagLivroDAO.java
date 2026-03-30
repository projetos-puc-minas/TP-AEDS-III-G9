package src.dao;

import src.util.Arquivo;
import src.model.TagLivro;
import java.util.ArrayList;

public class TagLivroDAO {
    private Arquivo<TagLivro> arqTags;

    public TagLivroDAO() throws Exception {
        arqTags = new Arquivo<>("tags_livros", TagLivro.class.getConstructor());
    }

    // -------------------------------------------------------
    // BUSCA POR ID
    // -------------------------------------------------------

    public TagLivro buscarTagId(int id) throws Exception {
        return arqTags.read(id);
    }

    // -------------------------------------------------------
    // BUSCA POR LIVRO — obrigatório para o relacionamento 1:N
    // Retorna todas as tags ativas de um determinado livro
    // -------------------------------------------------------

    public ArrayList<TagLivro> buscarTagsPorLivro(int idLivro) throws Exception {
        ArrayList<TagLivro> resultado = new ArrayList<>();
        ArrayList<TagLivro> todas = arqTags.readAll();

        for (TagLivro t : todas) {
            if (t.getLapide() && t.getIdLivro() == idLivro) {
                resultado.add(t);
            }
        }
        return resultado;
    }

    // -------------------------------------------------------
    // LISTAR TODAS AS TAGS ATIVAS
    // -------------------------------------------------------

    public ArrayList<TagLivro> listarTodas() throws Exception {
        ArrayList<TagLivro> resultado = new ArrayList<>();
        ArrayList<TagLivro> todas = arqTags.readAll();

        for (TagLivro t : todas) {
            if (t.getLapide()) {
                resultado.add(t);
            }
        }
        return resultado;
    }

    // -------------------------------------------------------
    // INCLUIR
    // -------------------------------------------------------

    public boolean incluirTag(TagLivro tagLivro) throws Exception {
        return arqTags.create(tagLivro) > 0;
    }

    // -------------------------------------------------------
    // ALTERAR
    // -------------------------------------------------------

    public boolean alterarTag(TagLivro tagLivro) throws Exception {
        return arqTags.update(tagLivro);
    }

    // -------------------------------------------------------
    // EXCLUIR POR ID
    // -------------------------------------------------------

    public boolean excluirTag(int id) throws Exception {
        return arqTags.delete(id);
    }

    // -------------------------------------------------------
    // EXCLUIR TODAS AS TAGS DE UM LIVRO
    // Útil ao deletar um livro (integridade referencial)
    // -------------------------------------------------------

    public boolean excluirTagsPorLivro(int idLivro) throws Exception {
        ArrayList<TagLivro> tags = buscarTagsPorLivro(idLivro);
        boolean sucesso = true;

        for (TagLivro t : tags) {
            if (!arqTags.delete(t.getId())) {
                sucesso = false;
            }
        }
        return sucesso;
    }
}