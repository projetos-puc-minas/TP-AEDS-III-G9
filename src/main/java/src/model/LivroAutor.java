package src.model;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import src.util.Registro;


public class LivroAutor implements Registro {

    private boolean lapide;
    private int     tamRegistro;
    private int     id;
    private int     idLivro;
    private int     idAutor;

    // Construtores

    public LivroAutor() {
        this(-1, -1, -1);
    }

    public LivroAutor(int idLivro, int idAutor) {
        this(-1, idLivro, idAutor);
    }

    public LivroAutor(int id, int idLivro, int idAutor) {
        this.lapide  = true;
        this.id      = id;
        this.idLivro = idLivro;
        this.idAutor = idAutor;
    }

    // Implementação da Interface Registro

    @Override public void    setLapide(boolean lapide) { this.lapide = lapide; }
    @Override public boolean getLapide()               { return this.lapide; }

    @Override public void    setTamRegistro(int tam)   { this.tamRegistro = tam; }
    @Override public int     getTamRegistro()          { return this.tamRegistro; }

    @Override public void    setId(int id)             { this.id = id; }
    @Override public int     getId()                   { return this.id; }

    // Getters e Setters

    public int  getIdLivro()               { return idLivro; }
    public void setIdLivro(int idLivro)    { this.idLivro = idLivro; }

    public int  getIdAutor()               { return idAutor; }
    public void setIdAutor(int idAutor)    { this.idAutor = idAutor; }

    // Serialização 

    @Override
    public byte[] toByteArray() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream      dos  = new DataOutputStream(baos);
        
        dos.writeInt(id);
        dos.writeInt(idLivro);
        dos.writeInt(idAutor);
        
        return baos.toByteArray();
    }

    @Override
    public void fromByteArray(byte[] b) throws IOException {
        ByteArrayInputStream bais = new ByteArrayInputStream(b);
        DataInputStream      dis  = new DataInputStream(bais);
        
        this.id      = dis.readInt();
        this.idLivro = dis.readInt();
        this.idAutor = dis.readInt();
    }

    @Override
    public String toString() {
        return "\nVínculo ID..: " + id
             + "\nID Livro....: " + idLivro
             + "\nID Autor....: " + idAutor;
    }
}