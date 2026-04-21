package src.model;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import src.util.Registro;
import src.util.SerializadorUtil;

/**
 * CORREÇÃO: serialização migrada de writeUTF/readUTF para SerializadorUtil
 * (delimitador ';'), alinhando com todos os outros modelos e com o esquema
 * binário documentado.
 *
 * A senha é armazenada já criptografada via XOR (processada em UsuarioService
 * antes de ser atribuída ao campo senhaXor).
 */
public class Usuarios implements Registro {

    private boolean lapide;
    private int     tamRegistro;
    private int     id;
    private String  nome;
    private String  email;
    private String  senhaXor; // armazenada criptografada

    // --- Construtores ---

    public Usuarios() {
        this(-1, "", "", "");
    }

    public Usuarios(int id, String nome, String email) {
        this(id, nome, email, "");
    }

    public Usuarios(int id, String nome, String email, String senhaXor) {
        this.lapide   = true;
        this.id       = id;
        this.nome     = nome;
        this.email    = email;
        this.senhaXor = senhaXor;
    }

    // --- Registro ---

    @Override public void    setLapide(boolean lapide) { this.lapide = lapide; }
    @Override public boolean getLapide()               { return this.lapide; }
    @Override public void    setTamRegistro(int tam)   { this.tamRegistro = tam; }
    @Override public int     getTamRegistro()          { return this.tamRegistro; }
    @Override public void    setId(int id)             { this.id = id; }
    @Override public int     getId()                   { return this.id; }

    // --- Getters / Setters ---

    public String getNome()                     { return nome; }
    public void   setNome(String nome)          { this.nome = nome; }

    public String getEmail()                    { return email; }
    public void   setEmail(String email)        { this.email = email; }

    public String getSenhaXor()                 { return senhaXor; }
    public void   setSenhaXor(String senhaXor)  { this.senhaXor = senhaXor; }

    // --- Serialização ---

    @Override
    public byte[] toByteArray() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream      dos  = new DataOutputStream(baos);

        dos.writeInt(id);
        SerializadorUtil.writeString(dos, nome);
        SerializadorUtil.writeString(dos, email);
        SerializadorUtil.writeString(dos, senhaXor);

        return baos.toByteArray();
    }

    @Override
    public void fromByteArray(byte[] b) throws IOException {
        ByteArrayInputStream bais = new ByteArrayInputStream(b);
        DataInputStream      dis  = new DataInputStream(bais);

        this.id       = dis.readInt();
        this.nome     = SerializadorUtil.readString(dis);
        this.email    = SerializadorUtil.readString(dis);
        this.senhaXor = SerializadorUtil.readString(dis);
    }

    @Override
    public String toString() {
        return "\nID......: " + id
             + "\nNome....: " + nome
             + "\nEmail...: " + email;
    }
}
