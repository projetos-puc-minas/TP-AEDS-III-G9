package src.model;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import src.util.Registro;
import src.util.SerializadorUtil;

/**
 * Modelo de Utilizador do sistema.
 *
 * A senha é armazenada de forma segura utilizando criptografia XOR.
 *
 * CAMPO MULTIVALORADO: redesSociais (String[])
 * Segue o mesmo padrão de serialização do campo generos em Livro,
 * usando SerializadorUtil.writeStringArray / readStringArray.
 */
public class Usuarios implements Registro {

    private boolean  lapide;
    private int      tamRegistro;
    private int      id;
    private String   nome;
    private String   email;
    private String   senhaXor;       // Armazenada criptografada em Base64
    private String[] redesSociais;   // Campo multivalorado — ex: ["instagram.com/user", "twitter.com/user"]

    // -------------------------------------------------------------------------
    // Construtores
    // -------------------------------------------------------------------------

    public Usuarios() {
        this(-1, "", "", "", new String[0]);
    }

    public Usuarios(int id, String nome, String email) {
        this(id, nome, email, "", new String[0]);
    }

    public Usuarios(int id, String nome, String email, String senhaXor) {
        this(id, nome, email, senhaXor, new String[0]);
    }

    public Usuarios(int id, String nome, String email, String senhaXor, String[] redesSociais) {
        this.lapide       = true;
        this.id           = id;
        this.nome         = nome;
        this.email        = email;
        this.senhaXor     = senhaXor;
        this.redesSociais = redesSociais != null ? redesSociais : new String[0];
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

    public String   getNome()                           { return nome; }
    public void     setNome(String nome)                { this.nome = nome; }

    public String   getEmail()                          { return email; }
    public void     setEmail(String email)              { this.email = email; }

    public String   getSenhaXor()                       { return senhaXor; }
    public void     setSenhaXor(String senhaXor)        { this.senhaXor = senhaXor; }

    public String[] getRedesSociais()                   { return redesSociais; }
    public void     setRedesSociais(String[] rs)        { this.redesSociais = rs != null ? rs : new String[0]; }

    // -------------------------------------------------------------------------
    // Serialização — campo redesSociais usa writeStringArray/readStringArray
    // -------------------------------------------------------------------------

    @Override
    public byte[] toByteArray() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream      dos  = new DataOutputStream(baos);

        dos.writeInt(id);
        SerializadorUtil.writeString(dos, nome);
        SerializadorUtil.writeString(dos, email);
        SerializadorUtil.writeString(dos, senhaXor);
        SerializadorUtil.writeStringArray(dos, redesSociais);

        return baos.toByteArray();
    }

    @Override
    public void fromByteArray(byte[] b) throws IOException {
        ByteArrayInputStream bais = new ByteArrayInputStream(b);
        DataInputStream      dis  = new DataInputStream(bais);

        this.id           = dis.readInt();
        this.nome         = SerializadorUtil.readString(dis);
        this.email        = SerializadorUtil.readString(dis);
        this.senhaXor     = SerializadorUtil.readString(dis);

        // Compatibilidade retroativa: se não houver mais bytes, array vazio
        if (dis.available() > 0) {
            this.redesSociais = SerializadorUtil.readStringArray(dis);
        } else {
            this.redesSociais = new String[0];
        }
    }

    @Override
    public String toString() {
        String rs = (redesSociais != null && redesSociais.length > 0)
                ? String.join(", ", redesSociais) : "(nenhuma)";
        return "\nID..............: " + id
             + "\nNome............: " + nome
             + "\nEmail...........: " + email
             + "\nRedes Sociais...: " + rs;
    }
}