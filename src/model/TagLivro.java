package src.model;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import src.util.Registro;

public class TagLivro implements Registro {

    private int id;
    private int idLivro;
    private int idTag;
    private boolean lapide;
    private int tamRegistro;

    public TagLivro() {
        this(-1, -1, -1);
    }

    public TagLivro(int idLivro, int idTag) {
        this(-1, idLivro, idTag);
    }

    public TagLivro(int id, int idLivro, int idTag) {
        this.id      = id;
        this.idLivro = idLivro;
        this.idTag   = idTag;
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

    public int getIdLivro() { return idLivro; }
    public void setIdLivro(int idLivro) { this.idLivro = idLivro; }

    public int getIdTag() { return idTag; }
    public void setIdTag(int idTag) { this.idTag = idTag; }

    // --- serialização ---

    @Override
    public byte[] toByteArray() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);

        dos.writeInt(this.id);
        dos.writeInt(this.idLivro);
        dos.writeInt(this.idTag);

        return baos.toByteArray();
    }

    @Override
    public void fromByteArray(byte[] b) throws IOException {
        ByteArrayInputStream bais = new ByteArrayInputStream(b);
        DataInputStream dis = new DataInputStream(bais);

        this.id      = dis.readInt();
        this.idLivro = dis.readInt();
        this.idTag   = dis.readInt();
    }

    @Override
    public String toString() {
        return "\nVínculo ID....: " + this.id +
               "\nLivro " + this.idLivro + " <---> Tag " + this.idTag;
    }
}