package src.util;

/**
 * Interface padrão para indexadores (ex: Árvore B+, Hash Extensível).
 * Define o contrato base para operações de índice em memória secundária,
 * mapeando chaves primárias (id) para endereços físicos (offsets) nos arquivos de dados.
 */
public interface Indexador {

    /**
     * Insere um novo par chave-endereço no índice.
     *
     * @param chave    Chave primária (id) do registro.
     * @param endereco Endereço físico (offset) em bytes no arquivo de dados (.bin).
     */
    void inserir(int chave, long endereco) throws Exception;

    /**
     * Busca o endereço físico correspondente a uma chave.
     *
     * @param chave Chave primária a ser buscada.
     * @return O offset físico no arquivo de dados, ou -1 (NULO) se não for encontrado.
     */
    long buscar(int chave) throws Exception;

    /**
     * Remove uma chave do índice.
     *
     * @param chave Chave a ser removida.
     * @return true se a chave foi encontrada e removida, false caso contrário.
     */
    boolean remover(int chave) throws Exception;

    /**
     * Atualiza o endereço físico (offset) associado a uma chave existente.
     * Necessário quando um registro cresce após um update e é movido para o final do arquivo de dados.
     *
     * @param chave        Chave do registro.
     * @param novoEndereco Novo offset físico no arquivo de dados.
     * @return true se atualizado com sucesso, false se a chave não for encontrada.
     */
    boolean atualizar(int chave, long novoEndereco) throws Exception;

    /**
     * Lista todos os registros indexados em ordem crescente.
     * Útil para travessia em índices como a Árvore B+ (percorrendo as folhas).
     *
     * @return Uma matriz 2D onde cada linha contém [chave, endereco].
     */
    long[][] listarOrdenado() throws Exception;

    /**
     * Retorna a quantidade total de chaves válidas armazenadas no índice.
     *
     * @return Total de chaves registradas.
     */
    long getTotalChaves() throws Exception;

    /**
     * Salva as alterações e fecha os recursos (arquivos físicos) manipulados pelo indexador.
     */
    void close() throws Exception;
}