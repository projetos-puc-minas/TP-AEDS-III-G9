package src.model;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import src.util.Registro;
import src.util.SerializadorUtil;

public class Livro implements Registro {
    private boolean lapide;
    private int tamRegistro;
    private int id;
    private int id_editora;
    private String titulo;
    private char[] isbn; // 13 caracteres
    private int ano_publicacao;
    private double preco;
    private String sinopse;

    // Construtor vazio (obrigatório para a classe Arquivo instanciar)
    public Livro() {
        this(-1, -1, "", new char[13], 0, 0.0, "");
    }

    // Construtor sem ID (para inserção)
    public Livro(int id_editora, String titulo, char[] isbn, int ano_publicacao, double preco, String sinopse) {
        this(-1, id_editora, titulo, isbn, ano_publicacao, preco, sinopse);
    }

    // Construtor completo
    public Livro(int id, int id_editora, String titulo, char[] isbn, int ano_publicacao, double preco, String sinopse) {
        // CORREÇÃO: lapide inicializada como true (registro ativo)
        this.lapide = true;
        this.id = id;
        this.id_editora = id_editora;
        this.titulo = titulo;
        this.isbn = isbn;
        this.ano_publicacao = ano_publicacao;
        this.preco = preco;
        this.sinopse = sinopse;
    }

    // --- Implementação da interface Registro ---

    @Override
    public void setLapide(boolean lapide) {
        this.lapide = lapide;
    }

    @Override
    public boolean getLapide() {
        return this.lapide;
    }

    @Override
    public void setTamRegistro(int tam) {
        this.tamRegistro = tam;
    }

    @Override
    public int getTamRegistro() {
        return this.tamRegistro;
    }

    @Override
    public void setId(int id) {
        this.id = id;
    }

    @Override
    public int getId() {
        return this.id;
    }

    // --- Getters e Setters específicos de Livro ---

    public int getIdEditora() { return id_editora; }
    public void setIdEditora(int id_editora) { this.id_editora = id_editora; }

    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }

    public char[] getIsbn() { return isbn; }
    public void setIsbn(char[] isbn) { this.isbn = isbn; }

    public int getAnoPublicacao() { return ano_publicacao; }
    public void setAnoPublicacao(int ano_publicacao) { this.ano_publicacao = ano_publicacao; }

    public double getPreco() { return preco; }
    public void setPreco(double preco) { this.preco = preco; }

    public String getSinopse() { return sinopse; }
    public void setSinopse(String sinopse) { this.sinopse = sinopse; }

    // --- Serialização e Desserialização ---

    @Override
    public byte[] toByteArray() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);

        dos.writeInt(this.id);
        dos.writeInt(this.id_editora);
        SerializadorUtil.writeString(dos, this.titulo);
        SerializadorUtil.writeIsbn(dos, this.isbn);
        dos.writeInt(this.ano_publicacao);
        dos.writeDouble(this.preco);
        SerializadorUtil.writeString(dos, this.sinopse);

        return baos.toByteArray();
    }

    @Override
    public void fromByteArray(byte[] b) throws IOException {
        ByteArrayInputStream bais = new ByteArrayInputStream(b);
        DataInputStream dis = new DataInputStream(bais);

        this.id = dis.readInt();
        this.id_editora = dis.readInt();
        this.titulo = SerializadorUtil.readString(dis);
        this.isbn = SerializadorUtil.readIsbn(dis);
        this.ano_publicacao = dis.readInt();
        this.preco = dis.readDouble();
        this.sinopse = SerializadorUtil.readString(dis);
    }

    @Override
    public String toString() {
        return "\nID...............: " + this.id +
               "\nID Editora.......: " + this.id_editora +
               "\nTítulo...........: " + this.titulo +
               "\nISBN.............: " + new String(this.isbn) +
               "\nAno de Publicação: " + this.ano_publicacao +
               "\nPreço............: R$ " + this.preco +
               "\nSinopse..........: " + this.sinopse;
    }
}