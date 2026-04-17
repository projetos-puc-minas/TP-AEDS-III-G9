package src.util;

public interface Indexador {

    void inserir(int chave, long endereco) throws Exception;

    long buscar(int chave) throws Exception;

    boolean remover(int chave) throws Exception;

    boolean atualizar(int chave, long novoEndereco) throws Exception;

    //long[][] listarOrdenado() throws Exception;

    //long getTotalChaves() throws Exception;

    void close() throws Exception;
}

