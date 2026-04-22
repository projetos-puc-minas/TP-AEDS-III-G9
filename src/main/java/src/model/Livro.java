package src.model;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import src.util.Registro;
import src.util.SerializadorUtil;

/**
 * Modelo Principal: Livro.
 * * Demonstra a utilização de campos variados conforme exigido no enunciado:
 * - idEditora     : Chave estrangeira (relacionamento 1:N com Editora).
 * - isbn          : Campo de tamanho fixo (13 caracteres).
 * - generos       : Campo multivalorado (vetor de Strings).
 * * Toda a serialização delega o tratamento de Strings para o SerializadorUtil,
 * garantindo a integridade dos ficheiros binários através do delimitador ';'.
 */
public class Livro implements Registro {

    private boolean  lapide;
    private int      tamRegistro;
    private int      id;
    private int      idEditora;
    private String   titulo;
    private char[]   isbn;           // 13 caracteres fixos
    private int      anoPublicacao;
    private double   preco;
    private String   sinopse;
    private String[] generos;        // Campo multivalorado

    // -------------------------------------------------------------------------
    // Construtores
    // -------------------------------------------------------------------------

    public Livro() {
        this(-1, -1, "", new char[13], 0, 0.0, "", new String[0]);
    }

    public Livro(int idEditora, String titulo, char[] isbn, int anoPublicacao,
                 double preco, String sinopse, String[] generos) {
        this(-1, idEditora, titulo, isbn, anoPublicacao, preco, sinopse, generos);
    }

    public Livro(int id, int idEditora, String titulo, char[] isbn, int anoPublicacao,
                 double preco, String sinopse, String[] generos) {
        this.lapide        = true;
        this.id            = id;
        this.idEditora     = idEditora;
        this.titulo        = titulo;
        this.isbn          = isbn;
        this.anoPublicacao = anoPublicacao;
        this.preco         = preco;
        this.sinopse       = sinopse;
        this.generos       = generos;
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

    public int      getIdEditora()                    { return idEditora; }
    public void     setIdEditora(int idEditora)       { this.idEditora = idEditora; }

    public String   getTitulo()                       { return titulo; }
    public void     setTitulo(String titulo)          { this.titulo = titulo; }

    public char[]   getIsbn()                         { return isbn; }
    public void     setIsbn(char[] isbn)              { this.isbn = isbn; }

    public int      getAnoPublicacao()                { return anoPublicacao; }
    public void     setAnoPublicacao(int anoPublicacao) { this.anoPublicacao = anoPublicacao; }

    public double   getPreco()                        { return preco; }
    public void     setPreco(double preco)            { this.preco = preco; }

    public String   getSinopse()                      { return sinopse; }
    public void     setSinopse(String sinopse)        { this.sinopse = sinopse; }

    public String[] getGeneros()                      { return generos; }
    public void     setGeneros(String[] generos)      { this.generos = generos; }

    // -------------------------------------------------------------------------
    // Serialização (Padrão do Projeto)
    // -------------------------------------------------------------------------

    @Override
    public byte[] toByteArray() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream      dos  = new DataOutputStream(baos);

        dos.writeInt(id);
        dos.writeInt(idEditora);
        SerializadorUtil.writeString(dos, titulo);
        SerializadorUtil.writeIsbn(dos, isbn);
        dos.writeInt(anoPublicacao);
        dos.writeDouble(preco);
        SerializadorUtil.writeString(dos, sinopse);
        SerializadorUtil.writeStringArray(dos, generos); // Serializa o vetor de Strings

        return baos.toByteArray();
    }

    @Override
    public void fromByteArray(byte[] b) throws IOException {
        ByteArrayInputStream bais = new ByteArrayInputStream(b);
        DataInputStream      dis  = new DataInputStream(bais);

        this.id            = dis.readInt();
        this.idEditora     = dis.readInt();
        this.titulo        = SerializadorUtil.readString(dis);
        this.isbn          = SerializadorUtil.readIsbn(dis);
        this.anoPublicacao = dis.readInt();
        this.preco         = dis.readDouble();
        this.sinopse       = SerializadorUtil.readString(dis);
        this.generos       = SerializadorUtil.readStringArray(dis);
    }

    // -------------------------------------------------------------------------
    // Impressão Formatada
    // -------------------------------------------------------------------------

    @Override
    public String toString() {
        String gStr = (generos != null && generos.length > 0)
                ? String.join(", ", generos) : "(nenhum)";
        return "\nID...............: " + id
             + "\nID Editora.......: " + idEditora
             + "\nTítulo...........: " + titulo
             + "\nISBN.............: " + new String(isbn)
             + "\nAno de Publicação: " + anoPublicacao
             + "\nPreço............: R$ " + preco
             + "\nSinopse..........: " + sinopse
             + "\nGéneros..........: " + gStr;
    }
}