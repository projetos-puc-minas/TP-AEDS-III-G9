package src.model;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.time.LocalDate;

import src.util.Registro;
import src.util.SerializadorUtil;

public class Editora implements Registro {
    private boolean lapide;
    private int tamRegistro;
    private int id;
    private String nome;
    private String cidade;
    private int ano_fundacao;

    // Construtor vazio (obrigatório para a classe Arquivo instanciar)
    public Editora() {
        this(-1, "", "", LocalDate.now().getYear());
    }

    // Construtor sem ID (para inserção)
    public Editora(String nome, String cidade, int ano_fundacao) {
        this(-1, nome, cidade, ano_fundacao);
    }

    // Construtor completo
    public Editora(int id, String nome, String cidade, int ano_fundacao) {
        // CORREÇÃO: lapide inicializada como true (registro ativo)
        this.lapide = true;
        this.id = id;
        this.nome = nome;
        this.cidade = cidade;
        this.ano_fundacao = ano_fundacao;
    }

    // --- Implementação da interface Registro ---

    @Override
    public void setLapide(boolean lapide) { this.lapide = lapide; }

    @Override
    public boolean getLapide() { return this.lapide; }

    @Override
    public void setTamRegistro(int tam) { this.tamRegistro = tam; }

    @Override
    public int getTamRegistro() { return this.tamRegistro; }

    @Override
    public void setId(int id) { this.id = id; }

    @Override
    public int getId() { return this.id; }

    // --- Getters e Setters específicos ---

    public void setNome(String nome) { this.nome = nome; }
    public String getNome() { return this.nome; }

    public void setCidade(String cidade) { this.cidade = cidade; }
    public String getCidade() { return this.cidade; }

    public void setAnoFundacao(int ano) { this.ano_fundacao = ano; }
    public int getAnoFundacao() { return this.ano_fundacao; }

    // --- Serialização e Desserialização ---

    @Override
    public byte[] toByteArray() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);

        dos.writeInt(this.id);
        SerializadorUtil.writeString(dos, this.nome);
        SerializadorUtil.writeString(dos, this.cidade);
        dos.writeInt(this.ano_fundacao);

        return baos.toByteArray();
    }

    @Override
    public void fromByteArray(byte[] b) throws IOException {
        ByteArrayInputStream bais = new ByteArrayInputStream(b);
        DataInputStream dis = new DataInputStream(bais);

        this.id = dis.readInt();
        this.nome = SerializadorUtil.readString(dis);
        this.cidade = SerializadorUtil.readString(dis);
        this.ano_fundacao = dis.readInt();
    }

    @Override
    public String toString() {
        return "\nID................: " + this.id +
               "\nNome..............: " + this.nome +
               "\nCidade............: " + this.cidade +
               "\nAno de Fundação...: " + this.ano_fundacao;
    }
}