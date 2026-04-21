package src.model;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import src.util.DataUtil;
import src.util.Registro;
import src.util.SerializadorUtil;

/**
 * CORREÇÃO: serialização migrada de writeUTF/readUTF para SerializadorUtil
 * (delimitador ';'), alinhando com todos os outros modelos do projeto e com
 * o esquema binário documentado.
 *
 * dataNascimento é armazenado como timestamp Unix em milissegundos (long, 8 bytes).
 * Use DataUtil.stringToTimestamp() e DataUtil.timestampToString() para conversão.
 */
public class Autores implements Registro {

    private boolean lapide;
    private int     tamRegistro;
    private int     id;
    private String  nome;
    private long    dataNascimento; // timestamp Unix em milissegundos
    private String  biografia;

    // --- Construtores ---

    public Autores() {
        this(-1, "", 0L, "");
    }

    public Autores(int id, String nome, long dataNascimento, String biografia) {
        this.lapide          = true;
        this.id              = id;
        this.nome            = nome;
        this.dataNascimento  = dataNascimento;
        this.biografia       = biografia;
    }

    // --- Registro ---

    @Override public void    setLapide(boolean lapide) { this.lapide = lapide; }
    @Override public boolean getLapide()               { return this.lapide; }
    @Override public void    setTamRegistro(int tam)   { this.tamRegistro = tam; }
    @Override public int     getTamRegistro()          { return this.tamRegistro; }
    @Override public void    setId(int id)             { this.id = id; }
    @Override public int     getId()                   { return this.id; }

    // --- Getters / Setters ---

    public String getNome()                         { return nome; }
    public void   setNome(String nome)              { this.nome = nome; }

    public long   getDataNascimento()               { return dataNascimento; }
    public void   setDataNascimento(long ts)        { this.dataNascimento = ts; }

    /** Retorna a data de nascimento no formato dd/MM/yyyy. */
    public String getDataNascimentoFormatada() {
        return DataUtil.timestampToString(dataNascimento);
    }

    public String getBiografia()                    { return biografia; }
    public void   setBiografia(String biografia)    { this.biografia = biografia; }

    // --- Serialização (padrão único do projeto: delimitador ';') ---

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

    @Override
    public String toString() {
        return "\nID................: " + id
             + "\nNome..............: " + nome
             + "\nData de Nascimento: " + getDataNascimentoFormatada()
             + "\nBiografia.........: " + biografia;
    }
}
