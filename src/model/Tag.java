package src.model;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import src.util.Registro;
import src.util.SerializadorUtil;

public class Tag implements Registro {

    private int id;
    private String nome;
    private boolean lapide;
    private int tamRegistro;

    public Tag() {
        this(-1, "");
    }

    public Tag(String nome) {
        this(-1, nome);
    }

    public Tag(int id, String nome) {
        this.id   = id;
        this.nome = nome;
    }

    // --- registro ---

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

    // --- getters / setters ---

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    // --- serialização ---

    @Override
    public byte[] toByteArray() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);

        dos.writeInt(this.id);
        SerializadorUtil.writeString(dos, this.nome);

        return baos.toByteArray();
    }

    @Override
    public void fromByteArray(byte[] b) throws IOException {
        ByteArrayInputStream bais = new ByteArrayInputStream(b);
        DataInputStream dis = new DataInputStream(bais);

        this.id   = dis.readInt();
        this.nome = SerializadorUtil.readString(dis);
    }

    @Override
    public String toString() {
        return "\nID........: " + this.id +
               "\nNome......: " + this.nome;
    }
}