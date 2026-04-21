package src.model;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import src.util.Registro;
import src.util.SerializadorUtil;

/**
 * Modelo de Livro.
 *
 * Campos:
 *   - id            : int
 *   - id_editora    : int  (FK → Editora, relacionamento 1:N)
 *   - titulo        : String
 *   - isbn          : char[13] (campo fixo de 13 bytes)
 *   - ano_publicacao: int
 *   - preco         : double  (campo real/ponto flutuante)
 *   - sinopse       : String
 *   - generos       : String[] (campo string multivalorado — requisito do enunciado)
 *
 * Serialização via SerializadorUtil (delimitador ';'), padrão único do projeto.
 * writeStringArray / readStringArray: 4 bytes (quantidade) + cada string com ';'.
 */
public class Livro implements Registro {

    private boolean  lapide;
    private int      tamRegistro;
    private int      id;
    private int      id_editora;
    private String   titulo;
    private char[]   isbn;           // 13 chars fixos
    private int      ano_publicacao;
    private double   preco;
    private String   sinopse;
    private String[] generos;        // campo multivalorado (ex.: ["Romance", "Drama"])

    // -------------------------------------------------------------------------
    // Construtores
    // -------------------------------------------------------------------------

    public Livro() {
        this(-1, -1, "", new char[13], 0, 0.0, "", new String[0]);
    }

    public Livro(int id_editora, String titulo, char[] isbn,
                 int ano_publicacao, double preco, String sinopse, String[] generos) {
        this(-1, id_editora, titulo, isbn, ano_publicacao, preco, sinopse, generos);
    }

    public Livro(int id, int id_editora, String titulo, char[] isbn,
                 int ano_publicacao, double preco, String sinopse, String[] generos) {
        this.lapide         = true;
        this.id             = id;
        this.id_editora     = id_editora;
        this.titulo         = titulo;
        this.isbn           = (isbn != null) ? isbn : new char[13];
        this.ano_publicacao = ano_publicacao;
        this.preco          = preco;
        this.sinopse        = sinopse;
        this.generos        = (generos != null) ? generos : new String[0];
    }

    // -------------------------------------------------------------------------
    // Registro
    // -------------------------------------------------------------------------

    @Override public void    setLapide(boolean lapide) { this.lapide = lapide; }
    @Override public boolean getLapide()               { return this.lapide; }
    @Override public void    setTamRegistro(int tam)   { this.tamRegistro = tam; }
    @Override public int     getTamRegistro()          { return this.tamRegistro; }
    @Override public void    setId(int id)             { this.id = id; }
    @Override public int     getId()                   { return this.id; }

    // -------------------------------------------------------------------------
    // Getters / Setters
    // -------------------------------------------------------------------------

    public int     getIdEditora()                 { return id_editora; }
    public void    setIdEditora(int id_editora)   { this.id_editora = id_editora; }

    public String  getTitulo()                    { return titulo; }
    public void    setTitulo(String titulo)       { this.titulo = titulo; }

    public char[]  getIsbn()                      { return isbn; }
    public void    setIsbn(char[] isbn)           { this.isbn = isbn; }

    public int     getAnoPublicacao()             { return ano_publicacao; }
    public void    setAnoPublicacao(int ano)      { this.ano_publicacao = ano; }

    public double  getPreco()                     { return preco; }
    public void    setPreco(double preco)         { this.preco = preco; }

    public String  getSinopse()                   { return sinopse; }
    public void    setSinopse(String sinopse)     { this.sinopse = sinopse; }

    public String[] getGeneros()                  { return generos; }
    public void     setGeneros(String[] generos)  { this.generos = (generos != null) ? generos : new String[0]; }

    // -------------------------------------------------------------------------
    // Serialização
    // -------------------------------------------------------------------------

    @Override
    public byte[] toByteArray() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream      dos  = new DataOutputStream(baos);

        dos.writeInt(id);
        dos.writeInt(id_editora);
        SerializadorUtil.writeString(dos, titulo);
        SerializadorUtil.writeIsbn(dos, isbn);
        dos.writeInt(ano_publicacao);
        dos.writeDouble(preco);
        SerializadorUtil.writeString(dos, sinopse);
        SerializadorUtil.writeStringArray(dos, generos); // campo multivalorado

        return baos.toByteArray();
    }

    @Override
    public void fromByteArray(byte[] b) throws IOException {
        ByteArrayInputStream bais = new ByteArrayInputStream(b);
        DataInputStream      dis  = new DataInputStream(bais);

        this.id             = dis.readInt();
        this.id_editora     = dis.readInt();
        this.titulo         = SerializadorUtil.readString(dis);
        this.isbn           = SerializadorUtil.readIsbn(dis);
        this.ano_publicacao = dis.readInt();
        this.preco          = dis.readDouble();
        this.sinopse        = SerializadorUtil.readString(dis);
        this.generos        = SerializadorUtil.readStringArray(dis);
    }

    @Override
    public String toString() {
        String gStr = (generos != null && generos.length > 0)
                ? String.join(", ", generos) : "(nenhum)";
        return "\nID...............: " + id
             + "\nID Editora.......: " + id_editora
             + "\nTítulo...........: " + titulo
             + "\nISBN.............: " + new String(isbn)
             + "\nAno de Publicação: " + ano_publicacao
             + "\nPreço............: R$ " + preco
             + "\nSinopse..........: " + sinopse
             + "\nGêneros..........: " + gStr;
    }
}