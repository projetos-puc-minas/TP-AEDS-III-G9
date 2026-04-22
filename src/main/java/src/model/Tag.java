package src.model;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import src.util.Registro;
import src.util.SerializadorUtil;


public class Tag implements Registro {

    private boolean lapide;
    private int     tamRegistro;
    private int     id;
    private String  nome;

    // Construtores

    public Tag() {
        this(-1, "");
    }

    public Tag(String nome) {
        this(-1, nome);
    }

    public Tag(int id, String nome) {
        this.lapide = true;
        this.id     = id;
        this.nome   = nome;
    }

    // Implementação da Interface Registro

    @Override public void    setLapide(boolean lapide) { this.lapide = lapide; }
    @Override public boolean getLapide()               { return this.lapide; }

    @Override public void    setTamRegistro(int tam)   { this.tamRegistro = tam; }
    @Override public int     getTamRegistro()          { return this.tamRegistro; }

    @Override public void    setId(int id)             { this.id = id; }
    @Override public int     getId()                   { return this.id; }

    // Getters e Setters

    public String getNome()            { return nome; }
    public void   setNome(String nome) { this.nome = nome; }

    // Serialização (Padrão do Projeto)

    @Override
    public byte[] toByteArray() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream      dos  = new DataOutputStream(baos);

        dos.writeInt(id);
        SerializadorUtil.writeString(dos, nome);

        return baos.toByteArray();
    }

    @Override
    public void fromByteArray(byte[] b) throws IOException {
        ByteArrayInputStream bais = new ByteArrayInputStream(b);
        DataInputStream      dis  = new DataInputStream(bais);

        this.id   = dis.readInt();
        this.nome = SerializadorUtil.readString(dis);
    }

    // Impressão Formatada

    @Override
    public String toString() {
        return "\nID......: " + id
             + "\nNome....: " + nome;
    }
}