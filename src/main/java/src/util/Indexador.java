package src.util;

//
//Interface padrão para indexadores (ex: Árvore B+, Hash Extensível).
//Define o contrato base para operações de índice em memória secundária,
//mapeando chaves primárias (id) para endereços físicos (offsets) nos arquivos de dados.
//
public interface Indexador {

    void inserir(int chave, long endereco) throws Exception;


    long buscar(int chave) throws Exception;

    boolean remover(int chave) throws Exception;

    boolean atualizar(int chave, long novoEndereco) throws Exception;

    long[][] listarOrdenado() throws Exception;


    long getTotalChaves() throws Exception;

    void close() throws Exception;
}