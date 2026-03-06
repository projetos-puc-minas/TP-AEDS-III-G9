package src.dao;

import src.util.Arquivo;
import src.model.Editora;

public class EditoraDAO {
    private Arquivo<Editora> arqEditoras;

    public EditoraDAO() throws Exception {
        arqEditoras = new Arquivo<>("editoras", Editora.class.getConstructor());
    }

    public Editora buscarEditoraId(int id) throws Exception {
        return arqEditoras.read(id);
    }

    //Implement these features later on
    /* 
     buscarTodasEditoras
     buscarEditoraPeloNome

    //Maybe this one
        BuscarEditora ---> search for multiple fields
    */

    public boolean incluirEditora(Editora editora) throws Exception {
        return arqEditoras.create(editora) > 0;
    }

    public boolean alterarEditora(Editora editora) throws Exception {
        return arqEditoras.update(editora);
    }

    public boolean excluirEditora(int id) throws Exception {
        return arqEditoras.delete(id);
    }
}
