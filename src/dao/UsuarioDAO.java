package src.dao;

import src.model.Usuarios;
import src.util.Arquivo;

public class UsuarioDAO
{
    private Arquivo<Usuarios> arqUsuarios;

    public UsuarioDAO() throws  Exception
    {
       arqUsuarios = new Arquivo<>("usuarios", Usuarios.class.getConstructor());

    }

    //CREAT
    public int incluirUsuario(Usuarios usuario) throws Exception
    {
        return arqUsuarios.create(usuario);
    }

    //READ
    public Usuarios buscarUsuario(int id) throws Exception
    {
        return arqUsuarios.read(id);
    }

    //UPDATE
    public boolean alterarUsuario(Usuarios usuario) throws Exception
    {
        return arqUsuarios.update(usuario);
    }

    //DELETE == desativa a lapide
    public boolean excluirUsuario(int id) throws Exception
    {
        return arqUsuarios.delete(id);
    }

}