package src.model;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import src.util.Registro;
import src.util.SerializadorUtil;

public class Editora implements Registro {

    private boolean lapide;
    private int     tamRegistro;
    private int     id;
    private String  nome;
    private String  cidade;
    private int     ano_fundacao;

    // --- Construtores ---

    public Editora() {
        this(-1, "", "", 0);
    }

    public Editora(String nome, String cidade, int ano_fundacao) {
        this(-1, nome, cidade, ano_fundacao);
    }

    public Editora(int id, String nome, String cidade, int ano_fundacao) {
        this.lapide       = true;
        this.id           = id;
        this.nome         = nome;
        this.cidade       = cidade;
        this.ano_fundacao = ano_fundacao;
    }

    // --- Registro ---

    @Override public void    setLapide(boolean lapide) { this.lapide = lapide; }
    @Override public boolean getLapide()               { return this.lapide; }
    @Override public void    setTamRegistro(int tam)   { this.tamRegistro = tam; }
    @Override public int     getTamRegistro()          { return this.tamRegistro; }
    @Override public void    setId(int id)             { this.id = id; }
    @Override public int     getId()                   { return this.id; }

    // --- Getters / Setters ---

    public String getNome()            { return nome; }
    public void   setNome(String nome) { this.nome = nome; }

    public String getCidade()              { return cidade; }
    public void   setCidade(String cidade) { this.cidade = cidade; }

    public int  getAnoFundacao()         { return ano_fundacao; }
    public void setAnoFundacao(int ano)  { this.ano_fundacao = ano; }

    // --- Serialização (padrão: SerializadorUtil com delimitador ';') ---

    @Override
    public byte[] toByteArray() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream      dos  = new DataOutputStream(baos);

        dos.writeInt(id);
        SerializadorUtil.writeString(dos, nome);
        SerializadorUtil.writeString(dos, cidade);
        dos.writeInt(ano_fundacao);

        return baos.toByteArray();
    }

    @Override
    public void fromByteArray(byte[] b) throws IOException {
        ByteArrayInputStream bais = new ByteArrayInputStream(b);
        DataInputStream      dis  = new DataInputStream(bais);

        this.id           = dis.readInt();
        this.nome         = SerializadorUtil.readString(dis);
        this.cidade       = SerializadorUtil.readString(dis);
        this.ano_fundacao = dis.readInt();
    }

    @Override
    public String toString() {
        return "\nID................: " + id
             + "\nNome..............: " + nome
             + "\nCidade............: " + cidade
             + "\nAno de Fundação...: " + ano_fundacao;
    }
}
