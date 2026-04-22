package src.util;

import java.io.IOException;

/**
 * Interface que define o contrato obrigatório para todas as entidades
 * que serão persistidas no gerenciador de Arquivo Binário Genérico.
 */
public interface Registro {

    /**
     * Define o status lógico do registro.
     * @param lapide true para ativo, false para excluído.
     */
    void setLapide(boolean lapide);

    /**
     * Retorna o status lógico do registro.
     * @return true se ativo, false se excluído.
     */
    boolean getLapide();

    /**
     * Define o tamanho físico do bloco que o registro ocupa no arquivo binário.
     * Importante: durante reaproveitamento de espaço, este valor representa o
     * tamanho do buraco (físico), e não apenas o tamanho dos dados úteis.
     * @param tam Tamanho em bytes.
     */
    void setTamRegistro(int tam);

    /**
     * Retorna o tamanho físico do bloco do registro no arquivo.
     * @return Tamanho em bytes.
     */
    int getTamRegistro();

    /**
     * Define o identificador único (chave primária) do registro.
     * @param id Identificador gerado pelo Arquivo.
     */
    void setId(int id);

    /**
     * Retorna o identificador único do registro.
     * @return id do registro.
     */
    int getId();

    /**
     * Serializa os atributos da entidade para um array de bytes.
     * Não inclui lápide nem tamanho (estes são controlados pelo Arquivo).
     * @return Array de bytes representando os dados da entidade.
     */
    byte[] toByteArray() throws IOException;

    /**
     * Preenche os atributos da entidade a partir de um array de bytes.
     * O array fornecido pode conter "lixo" no final caso o registro esteja
     * ocupando um bloco reaproveitado maior que o necessário. A leitura deve ser exata.
     * @param b Array de bytes lido do arquivo.
     */
    void fromByteArray(byte[] b) throws IOException;
}