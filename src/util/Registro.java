package src.util;

import java.io.IOException;

public interface Registro {

    // Armazenamento

    void setLapide(boolean lapide);
    boolean getLapide();

    void setTamRegistro(int tam);
    int getTamRegistro();

    // Identidade

    void setId(int id);
    int getId();

    // Serialização

    byte[] toByteArray() throws IOException;

    void fromByteArray(byte[] b) throws IOException;
}