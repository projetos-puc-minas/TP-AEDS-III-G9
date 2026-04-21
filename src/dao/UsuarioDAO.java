package src.dao;

import src.model.Usuarios;
import src.util.Arquivo;
import src.util.Indexador;

public class UsuarioDAO
{
    private Arquivo<Usuarios> arqUsuarios;
    private final Indexador indice;

    public UsuarioDAO(Indexador indice) throws Exception
    {
        arqUsuarios = new Arquivo<>("usuarios", Usuarios.class.getConstructor());
        this.indice = indice;
    }

    // CREATE
    public int incluirUsuario(Usuarios usuario) throws Exception
    {
        return arqUsuarios.create(usuario, indice);
    }

    // READ
    public Usuarios buscarUsuario(int id) throws Exception
    {
        return arqUsuarios.read(id, indice);
    }

    // UPDATE
    public boolean alterarUsuario(Usuarios usuario) throws Exception
    {
        return arqUsuarios.update(usuario, indice);
    }

    // DELETE == desativa a lapide
    public boolean excluirUsuario(int id) throws Exception
    {
        return arqUsuarios.delete(id, indice);
    }

}