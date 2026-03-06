package src.model;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.time.LocalDate;

import src.util.Registro;

public class Editora implements Registro{
    private int id;
    private String nome;
    private String cidade;
    private int ano_fundacao;
    private boolean lapide;
    private int tamRegistro;
    


    public Editora(){
        this(-1, "", "", LocalDate.now().getYear());
    }

    public Editora(String nome, String cidade, int ano_fundacao){
        this.nome = nome;
        this.cidade = cidade;
        this.ano_fundacao = ano_fundacao;
    }

    public Editora(int id, String nome, String cidade, int ano_fundacao){
        this.id = id;
        this.nome = nome;
        this.cidade = cidade;
        this.ano_fundacao = ano_fundacao;
    }

    public void setLapide(boolean lapide){
        this.lapide = lapide;
    }

    public boolean getLapide(){
        return this.lapide;
    }

    public void setTamRegistro(int tam){
        this.tamRegistro = tam;
    }

    public int getTamRegistro(){
        return this.tamRegistro;
    }


    public void setId(int id) {
        this.id = id;
    }

    public int getId(){
        return this.id;
    }

    public void setNome(String nome){
        this.nome = nome;
    }

    public String getNome(){
        return this.nome; 
    }

    public void setCidade(String cidade){
        this.cidade = cidade;
    }

    public String getCidade(String cidade){
        return this.cidade;
    }

    public void setAnoFundacao(int ano){
        this.ano_fundacao = ano;
    }

    public int getAnoFundacao(){
        return this.ano_fundacao;
    }

    public byte[] toByteArray() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);

        dos.writeInt(this.id);
        dos.writeUTF(this.nome);
        dos.writeUTF(this.cidade);
        dos.writeInt(this.ano_fundacao);

        return baos.toByteArray();
    }

    public void fromByteArray(byte[] b) throws IOException {
        ByteArrayInputStream bais = new ByteArrayInputStream(b);
        DataInputStream dis = new DataInputStream(bais);

        this.id = dis.readInt();
        this.nome = dis.readUTF();
        this.cidade = dis.readUTF();
        this.ano_fundacao = dis.readInt();
    }

    @Override
    public String toString() {
        return "\nID........: " + this.id +
        "\nNome......: " + this.nome +
        "\nCidade.......: " + this.cidade +
        "\nAno de Fundação...: " + this.ano_fundacao;
    }

}
