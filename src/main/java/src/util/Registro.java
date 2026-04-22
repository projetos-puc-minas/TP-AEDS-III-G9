package src.util;

import java.io.IOException;

/**
 * Interface que define o contrato obrigatório para todas as entidades
 * que serão persistidas no gerenciador de Arquivo Binário Genérico.
 */
public interface Registro {


    void setLapide(boolean lapide);

    boolean getLapide();


    void setTamRegistro(int tam);


    int getTamRegistro();

    void setId(int id);

    int getId();

    byte[] toByteArray() throws IOException;


    void fromByteArray(byte[] b) throws IOException;
}