package src.dao;

import src.model.Autores;
import src.util.Arquivo;

public class AutoresDAO {
    private Arquivo<Autores> arqAutores;

    public AutoresDAO() throws Exception {
        arqAutores = new Arquivo<>("autores", Autores.class.getConstructor());
    }

    // Create
    public int adicionarAutor(Autores autor) throws Exception {
        return arqAutores.create(autor);
    }

    // Read
    public Autores buscarAutor(int id) throws Exception {
        return arqAutores.read(id);
    }

    // Update
    public boolean alterarAutor(Autores autor) throws Exception {
        return arqAutores.update(autor);
    }

    // Delete = desativa a lápide
    public boolean excluirAutor(int id) throws Exception {
        return arqAutores.delete(id);
    }
}