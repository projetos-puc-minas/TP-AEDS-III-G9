package src.util;

public interface Indexador {

    /**
     * Insere um par (chave → endereço físico no .bin).
     * @param chave     chave primária (int)
     * @param endereco  offset em bytes no arquivo .bin onde o registro está gravado
     */
    void inserir(int chave, long endereco) throws Exception;

    /**
     * Retorna o endereço físico do registro com essa chave, ou -1 se não encontrado.
     */
    long buscar(int chave) throws Exception;

    boolean remover(int chave) throws Exception;

    boolean atualizar(int chave, long novoEndereco) throws Exception;

    /**
     * Retorna todos os pares [chave, endereço] em ordem crescente de chave,
     * percorrendo as folhas da árvore da esquerda para a direita.
     */
    long[][] listarOrdenado() throws Exception;

    long getTotalChaves() throws Exception;

    void close() throws Exception;
}
