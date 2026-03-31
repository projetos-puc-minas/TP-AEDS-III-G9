package src.model;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class Autores {
    // Controle
    boolean lapide; // true = ativo ; false = excluído
    int tamRegistro;

    // Informações do autor
    int id;
    String nome;
    long dataNascimento;
    String biografia;

    public Autores(int id, String nome, long dataNasciento, String biografia) {
        this.id = id;
        this.nome = nome;
        this.dataNascimento = dataNasciento;
        this.biografia = biografia;
    }

    // Funções set

    public void setLapide(boolean lapide) {
        this.lapide = lapide;
    }
    
    public void setTamRegistro(int tam) {
        this.tamRegistro = tam;
    }

    public void setId(int id) {
        this.id = id;
    }
    
    public void setNome(String nome) {
        this.nome = nome;
    }
    
    public void setDataNascimento(long dataNascimento) {
        this.dataNascimento = dataNascimento;
    }

    public void setBiografia(String bio) {
        this.biografia = bio;
    }

    // Funções get

    public boolean getLapide() {
        return this.lapide;
    }

    public int getTamRegistro() {
        return this.tamRegistro;
    }

    public int getId() {
        return this.id;
    }

    public String getNome() {
        return this.nome;
    }

    public long getDataNascimento() {
        return this.dataNascimento;
    }

    public String getBiografia() {
        return this.biografia;
    }

    public byte[] toByteArray() throws IOException
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        
        dos.writeInt(id);
        dos.writeUTF(nome);
        dos.writeLong(dataNascimento);
        dos.writeUTF(biografia);
        
        return baos.toByteArray();
    }
    
    public void fromByteArray(byte[] ba) throws IOException
    {
        ByteArrayInputStream bais = new ByteArrayInputStream(ba);
        DataInputStream dis = new DataInputStream(bais);

        this.id = dis.readInt();
        this.nome = dis.readUTF();
        this.dataNascimento = dis.readLong();
        this.biografia = dis.readUTF();
    }

}