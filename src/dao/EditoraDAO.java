package src.dao;

import src.util.Arquivo;
import src.util.Indexador;
import src.model.Editora;

public class EditoraDAO {
    private Arquivo<Editora> arqEditoras;
    private final Indexador indice;

    public EditoraDAO(Indexador indice) throws Exception {
        arqEditoras = new Arquivo<>("editoras", Editora.class.getConstructor());
        this.indice = indice;
    }

    public Editora buscarEditoraId(int id) throws Exception {
        return arqEditoras.read(id, indice);
    }

    //Implement these features later on
    /* 
     buscarTodasEditoras
     buscarEditoraPeloNome

    //Maybe this one
        BuscarEditora ---> search for multiple fields
    */

    public boolean incluirEditora(Editora editora) throws Exception {
        return arqEditoras.create(editora, indice) > 0;
    }

    public boolean alterarEditora(Editora editora) throws Exception {
        return arqEditoras.update(editora, indice);
    }

    public boolean excluirEditora(int id) throws Exception {
        return arqEditoras.delete(id, indice);
    }
}
