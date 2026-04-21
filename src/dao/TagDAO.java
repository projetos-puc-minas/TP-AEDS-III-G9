package src.dao;

import java.util.List;
import src.model.Tag;
import src.util.Arquivo;
import src.util.Indexador;

public class TagDAO {

    private final Arquivo<Tag> arqTags;
    private final Indexador indice;

    public TagDAO(Indexador indice) throws Exception {
        this.arqTags = new Arquivo<>("tags", Tag.class.getConstructor());
        this.indice  = indice;
    }

    // --- create ---

    public int criar(Tag tag) throws Exception {
        return arqTags.create(tag, indice);
    }

    // --- read ---

    public Tag buscarPorId(int id) throws Exception {
        if (indice.buscar(id) < 0) {
            return null;
        }
        
        return arqTags.read(id, indice);
    }

    public List<Tag> listarTodos() throws Exception {
        return arqTags.listarTodos();
    }

    // --- update ---

    public boolean alterar(Tag tag) throws Exception {
        return arqTags.update(tag, indice);
    }

    // --- delete ---

    public boolean excluirPorId(int id) throws Exception {
        return arqTags.delete(id, indice);
    }
}