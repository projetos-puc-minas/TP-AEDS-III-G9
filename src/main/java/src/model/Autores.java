package src.model;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import src.util.DataUtil;
import src.util.Registro;
import src.util.SerializadorUtil;


public class Autores implements Registro {

    private boolean lapide;
    private int     tamRegistro;
    private int     id;
    private String  nome;
    private long    dataNascimento; // Armazenado como timestamp Unix em milissegundos
    private String  biografia;

    // Construtores

    public Autores() {
        this(-1, "", 0L, "");
    }

    public Autores(String nome, long dataNascimento, String biografia) {
        this(-1, nome, dataNascimento, biografia);
    }

    public Autores(int id, String nome, long dataNascimento, String biografia) {
        this.lapide         = true;
        this.id             = id;
        this.nome           = nome;
        this.dataNascimento = dataNascimento;
        this.biografia      = biografia;
    }

    // Implementação da Interface Registro

    @Override public void    setLapide(boolean lapide) { this.lapide = lapide; }
    @Override public boolean getLapide()               { return this.lapide; }

    @Override public void    setTamRegistro(int tam)   { this.tamRegistro = tam; }
    @Override public int     getTamRegistro()          { return this.tamRegistro; }

    @Override public void    setId(int id)             { this.id = id; }
    @Override public int     getId()                   { return this.id; }

    // Getters e Setters

    public String getNome()                            { return nome; }
    public void   setNome(String nome)                 { this.nome = nome; }

    public long   getDataNascimento()                  { return dataNascimento; }
    public void   setDataNascimento(long timestamp)    { this.dataNascimento = timestamp; }

    /** Retorna a data de nascimento já formatada em dd/MM/yyyy utilizando o DataUtil. */
    public String getDataNascimentoFormatada() {
        return DataUtil.timestampToString(dataNascimento);
    }

    public String getBiografia()                       { return biografia; }
    public void   setBiografia(String biografia)       { this.biografia = biografia; }

    // Serialização (Padrão do Projeto)

    @Override
    public byte[] toByteArray() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream      dos  = new DataOutputStream(baos);

        dos.writeInt(id);
        SerializadorUtil.writeString(dos, nome);
        dos.writeLong(dataNascimento);
        SerializadorUtil.writeString(dos, biografia);

        return baos.toByteArray();
    }

    @Override
    public void fromByteArray(byte[] b) throws IOException {
        ByteArrayInputStream bais = new ByteArrayInputStream(b);
        DataInputStream      dis  = new DataInputStream(bais);

        this.id             = dis.readInt();
        this.nome           = SerializadorUtil.readString(dis);
        this.dataNascimento = dis.readLong();
        this.biografia      = SerializadorUtil.readString(dis);
    }

    // Impressão Formatada

    @Override
    public String toString() {
        return "\nID..................: " + id
             + "\nNome................: " + nome
             + "\nData de Nascimento..: " + getDataNascimentoFormatada()
             + "\nBiografia...........: " + biografia;
    }
}