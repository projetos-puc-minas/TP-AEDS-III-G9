package src.util;

import java.io.IOException;

public interface Registro {

    // --- armazenamento ---

    void setLapide(boolean lapide);
    boolean getLapide();

    void setTamRegistro(int tam);
    int getTamRegistro();

    // --- identidade ---

    void setId(int id);
    int getId();

    // --- serialização ---

    byte[] toByteArray() throws IOException;

    void fromByteArray(byte[] b) throws IOException;
}