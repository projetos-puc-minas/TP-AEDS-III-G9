package src.model;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import src.util.Registro;

/**
 * Modelo da Tabela Intermediária (Relacionamento N:N).
 * Vincula uma Tag a um Livro através das chaves estrangeiras (idTag e idLivro).
 * Como contém apenas inteiros, o tamanho após serialização é fixo (12 bytes de dados).
 */
public class TagsLivros implements Registro {

    private boolean lapide;
    private int     tamRegistro;
    private int     id;
    private int     idTag;
    private int     idLivro;

    // -------------------------------------------------------------------------
    // Construtores
    // -------------------------------------------------------------------------

    public TagsLivros() {
        this(-1, -1, -1);
    }

    public TagsLivros(int idTag, int idLivro) {
        this(-1, idTag, idLivro);
    }

    public TagsLivros(int id, int idTag, int idLivro) {
        this.lapide  = true;
        this.id      = id;
        this.idTag   = idTag;
        this.idLivro = idLivro;
    }

    // -------------------------------------------------------------------------
    // Implementação da Interface Registro
    // -------------------------------------------------------------------------

    @Override public void    setLapide(boolean lapide) { this.lapide = lapide; }
    @Override public boolean getLapide()               { return this.lapide; }

    @Override public void    setTamRegistro(int tam)   { this.tamRegistro = tam; }
    @Override public int     getTamRegistro()          { return this.tamRegistro; }

    @Override public void    setId(int id)             { this.id = id; }
    @Override public int     getId()                   { return this.id; }

    // -------------------------------------------------------------------------
    // Getters e Setters
    // -------------------------------------------------------------------------

    public int  getIdTag()               { return idTag; }
    public void setIdTag(int idTag)      { this.idTag = idTag; }

    public int  getIdLivro()             { return idLivro; }
    public void setIdLivro(int idLivro)  { this.idLivro = idLivro; }

    // -------------------------------------------------------------------------
    // Serialização (Tamanho Fixo — apenas ints)
    // -------------------------------------------------------------------------

    @Override
    public byte[] toByteArray() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream      dos  = new DataOutputStream(baos);

        dos.writeInt(id);
        dos.writeInt(idTag);
        dos.writeInt(idLivro);

        return baos.toByteArray();
    }

    @Override
    public void fromByteArray(byte[] b) throws IOException {
        ByteArrayInputStream bais = new ByteArrayInputStream(b);
        DataInputStream      dis  = new DataInputStream(bais);

        this.id      = dis.readInt();
        this.idTag   = dis.readInt();
        this.idLivro = dis.readInt();
    }

    @Override
    public String toString() {
        return "\nVínculo ID..: " + id
             + "\nID Tag......: " + idTag
             + "\nID Livro....: " + idLivro;
    }
}