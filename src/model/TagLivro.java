package src.model;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import src.util.Registro;
import src.util.SerializadorUtil;

public class TagLivro implements Registro {
    private boolean lapide;
    private int tamRegistro;
    private int id;
    private int id_livro;
    private String tag;

    // Construtor vazio (exigido pela classe Arquivo)
    public TagLivro() {
        this(-1, -1, "");
    }

    // Construtor sem ID (para inserção)
    public TagLivro(int id_livro, String tag) {
        this(-1, id_livro, tag);
    }

    // Construtor completo
    public TagLivro(int id, int id_livro, String tag) {
        // CORREÇÃO: lapide inicializada como true (registro ativo)
        this.lapide = true;
        this.id = id;
        this.id_livro = id_livro;
        this.tag = tag;
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
    public int getIdLivro() { return id_livro; }
    public void setIdLivro(int id_livro) { this.id_livro = id_livro; }
    public String getTag() { return tag; }
    public void setTag(String tag) { this.tag = tag; }

    // --- Serialização e Desserialização ---
    @Override
    public byte[] toByteArray() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);

        dos.writeInt(this.id);
        dos.writeInt(this.id_livro);
        SerializadorUtil.writeString(dos, this.tag);

        return baos.toByteArray();
    }

    @Override
    public void fromByteArray(byte[] b) throws IOException {
        ByteArrayInputStream bais = new ByteArrayInputStream(b);
        DataInputStream dis = new DataInputStream(bais);

        this.id = dis.readInt();
        this.id_livro = dis.readInt();
        this.tag = SerializadorUtil.readString(dis);
    }

    @Override
    public String toString() {
        return "TagLivro [ID=" + id + ", ID_Livro=" + id_livro + ", Tag=" + tag + "]";
    }
}