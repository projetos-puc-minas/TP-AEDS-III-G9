package src.dao;

import src.util.Arquivo;
import src.model.Editora;
import java.util.ArrayList;

public class EditoraDAO {
    private Arquivo<Editora> arqEditoras;

    public EditoraDAO() throws Exception {
        arqEditoras = new Arquivo<>("editoras", Editora.class.getConstructor());
    }

    // -------------------------------------------------------
    // BUSCA POR ID
    // -------------------------------------------------------

    public Editora buscarEditoraId(int id) throws Exception {
        return arqEditoras.read(id);
    }

    // -------------------------------------------------------
    // LISTAR TODAS AS EDITORAS ATIVAS
    // -------------------------------------------------------

    public ArrayList<Editora> listarTodasEditoras() throws Exception {
        ArrayList<Editora> resultado = new ArrayList<>();
        ArrayList<Editora> todas = arqEditoras.readAll();

        for (Editora e : todas) {
            if (e.getLapide()) {
                resultado.add(e);
            }
        }
        return resultado;
    }

    // -------------------------------------------------------
    // INCLUIR
    // -------------------------------------------------------

    public boolean incluirEditora(Editora editora) throws Exception {
        return arqEditoras.create(editora) > 0;
    }

    // -------------------------------------------------------
    // ALTERAR
    // -------------------------------------------------------

    public boolean alterarEditora(Editora editora) throws Exception {
        return arqEditoras.update(editora);
    }

    // -------------------------------------------------------
    // EXCLUIR
    // -------------------------------------------------------

    public boolean excluirEditora(int id) throws Exception {
        return arqEditoras.delete(id);
    }
}