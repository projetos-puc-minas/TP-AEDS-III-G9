package src.model;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import src.util.Registro;



public class Usuarios implements Registro
{
    boolean lapide;
    int tamRegistro;
    int id;
    String nome;
    String email;
    String senhaXor;

    public Usuarios()
    {
        this.id = -1;
        this.nome = "";
        this.email = "";
        this.senhaXor = "";
        this.lapide = false;
    }

    public Usuarios(int id, String nome, String email)
    {
        this.lapide = false;
        this.id = id;
        this.nome = nome;
        this.email = email;
        this.senhaXor = "";
    }

    //setters
    public void setLapide(boolean Lapide)
    {
        this.lapide = lapide;
    }

    public void setNome(String nome)
    {
        this.nome = nome;
    }

    public void setId(int id)
    {
        this.id = id;
    }

    public void setEmail(String email)
    {
        this.email = email;
    }

    public void setTamRegistro(int tamRegistros)
    {
        this.tamRegistro = tamRegistros;
    }

    public void SetSenhaXor(String senhaXor)
    {
        this.senhaXor = senhaXor;
    }




    //getters
    public boolean getLapide()
    {
        return this.lapide;
    }

    public String getNome()
    {
        return this.nome;
    }

    public int getId()
    {
        return this.id;
    }

    public String getEmail()
    {
        return this.email;
    }

    public int getTamRegistro()
    {
        return this.tamRegistro;
    }

    public String getSenhaXor()
    {
        return this.senhaXor;
    }


    public byte[] toByteArray() throws IOException
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        
        dos.writeInt(id);
        dos.writeUTF(nome);
        dos.writeUTF(email);
        dos.writeUTF(senhaXor);
        
        return baos.toByteArray();
    }
    
    public void fromByteArray(byte[] ba) throws IOException
    {
        ByteArrayInputStream bais = new ByteArrayInputStream(ba);
        DataInputStream dis = new DataInputStream(bais);

        this.id = dis.readInt();
        this.nome = dis.readUTF();
        this.email = dis.readUTF();
        this.senhaXor = dis.readUTF();
    }
}