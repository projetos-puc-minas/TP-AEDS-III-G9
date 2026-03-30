package src.model;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import src.util.Registro;

public class LivroAutor implements Registro {
    private boolean lapide;
    private int tamRegistro;
    private int id;
    private int id_livro;
    private int id_autor;

    // Construtor vazio (exigido pela classe Arquivo)
    public LivroAutor() {
        this(-1, -1, -1);
    }

    // Construtor sem ID (para inserção)
    public LivroAutor(int id_livro, int id_autor) {
        this(-1, id_livro, id_autor);
    }

    // Construtor completo
    public LivroAutor(int id, int id_livro, int id_autor) {
        this.lapide   = true;
        this.id       = id;
        this.id_livro = id_livro;
        this.id_autor = id_autor;
    }

    // --- Implementação da interface Registro ---

    @Override public void setLapide(boolean lapide)  { this.lapide = lapide; }
    @Override public boolean getLapide()              { return this.lapide; }
    @Override public void setTamRegistro(int tam)     { this.tamRegistro = tam; }
    @Override public int getTamRegistro()             { return this.tamRegistro; }
    @Override public void setId(int id)               { this.id = id; }
    @Override public int getId()                      { return this.id; }

    // --- Getters e Setters específicos ---

    public int getIdLivro()            { return id_livro; }
    public void setIdLivro(int id)     { this.id_livro = id; }
    public int getIdAutor()            { return id_autor; }
    public void setIdAutor(int id)     { this.id_autor = id; }

    // --- Serialização e Desserialização ---
    // Registro de tamanho fixo: 3 ints = 12 bytes (sem strings)

    @Override
    public byte[] toByteArray() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);

        dos.writeInt(this.id);
        dos.writeInt(this.id_livro);
        dos.writeInt(this.id_autor);

        return baos.toByteArray();
    }

    @Override
    public void fromByteArray(byte[] b) throws IOException {
        ByteArrayInputStream bais = new ByteArrayInputStream(b);
        DataInputStream dis = new DataInputStream(bais);

        this.id       = dis.readInt();
        this.id_livro = dis.readInt();
        this.id_autor = dis.readInt();
    }

    @Override
    public String toString() {
        return "LivroAutor [ID=" + id + ", ID_Livro=" + id_livro + ", ID_Autor=" + id_autor + "]";
    }
}