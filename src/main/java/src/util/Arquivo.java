package src.util;

import java.io.File;
import java.io.RandomAccessFile;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;

/**
 * Gerenciador genérico de arquivo binário com suporte a:
 * - Cabeçalho fixo de 16 bytes
 * - Exclusão lógica via lápide
 * - Reaproveitamento de espaço via lista encadeada best-fit ordenada por tamanho
 *
 * Layout do cabeçalho (16 bytes):
 *   [0..3]  int   ultimo_id              — maior ID já gerado
 *   [4..7]  int   total_registros        — quantidade de registros ativos
 *   [8..15] long  ponteiro_lista_excluidos — offset do 1º bloco livre (-1 = vazia)
 *
 * Layout de cada registro no arquivo:
 *   [0]     boolean  lapide        — true = ativo, false = excluído logicamente
 *   [1..4]  int      tam_registro  — tamanho físico do bloco reservado (em bytes)
 *   [5..]   byte[]   dados         — conteúdo serializado pelo próprio objeto
 *
 * Registros excluídos armazenam, nos primeiros 8 bytes de "dados",
 * o ponteiro (offset) para o próximo bloco excluído da lista encadeada.
 */
public class Arquivo<T extends Registro> {

    // ------------------------------------------------------------------
    // Constantes — offsets fixos do cabeçalho
    // ------------------------------------------------------------------
    public  static final int  TAM_CABECALHO     = 16;
    private static final int  OFFSET_ULTIMO_ID  = 0;
    private static final int  OFFSET_TOTAL_REG  = 4;
    private static final int  OFFSET_LISTA_EXC  = 8;

    // Offsets dentro de cada registro (relativos ao início do registro)
    private static final int  OFF_LAPIDE        = 0;
    private static final int  OFF_TAM           = 1;
    private static final int  OFF_DADOS         = 5;

    // Os 8 primeiros bytes de "dados" num bloco excluído guardam o próximo ponteiro
    private static final int  OFF_PROX_EXCLUIDO = OFF_DADOS;

    private static final boolean LAPIDE_ATIVO    = true;
    private static final boolean LAPIDE_EXCLUIDO = false;

    // Sentinela: indica ausência de ponteiro válido (lista vazia ou fim da lista)
    public static final long NULO = -1L;

    private final RandomAccessFile arquivo;
    private final Constructor<T>   construtor;

    // ------------------------------------------------------------------
    // Construtor
    // ------------------------------------------------------------------

    /**
     * Abre (ou cria) o arquivo binário na pasta ./data/.
     * Se o arquivo for novo, escreve o cabeçalho inicial zerado.
     *
     * @param nomeArquivo nome base do arquivo (sem extensão)
     * @param construtor  construtor padrão (sem parâmetros) da classe T
     */
    public Arquivo(String nomeArquivo, Constructor<T> construtor) throws Exception {
        File dir = new File("./data");
        if (!dir.exists()) dir.mkdirs();

        this.construtor = construtor;
        this.arquivo    = new RandomAccessFile("./data/" + nomeArquivo + ".bin", "rw");

        // Inicializa o cabeçalho se o arquivo acabou de ser criado
        if (arquivo.length() < TAM_CABECALHO) {
            arquivo.seek(0);
            arquivo.writeInt(0);     // ultimo_id
            arquivo.writeInt(0);     // total_registros
            arquivo.writeLong(NULO); // ponteiro_lista_excluidos (lista vazia)
        }
    }

    // ------------------------------------------------------------------
    // Classe auxiliar para retornar id + endereço físico após criação
    // ------------------------------------------------------------------

    public static class CreateResult {
        public final int  id;
        public final long endereco;

        CreateResult(int id, long endereco) {
            this.id       = id;
            this.endereco = endereco;
        }
    }

    // ------------------------------------------------------------------
    // CRUD
    // ------------------------------------------------------------------

    /**
     * Insere um novo registro no arquivo.
     * Tenta reaproveitar um bloco excluído (best-fit); caso não encontre,
     * acrescenta ao final do arquivo.
     *
     * @return CreateResult com o novo ID atribuído e o offset físico do registro
     */
    public CreateResult create(T obj) throws Exception {
        // Incrementa e obtém o próximo ID sequencial
        arquivo.seek(OFFSET_ULTIMO_ID);
        int novoID = arquivo.readInt() + 1;
        arquivo.seek(OFFSET_ULTIMO_ID);
        arquivo.writeInt(novoID);
        incrementaTotalRegistros(1);

        obj.setId(novoID);
        byte[] dados   = obj.toByteArray();
        int    tamDados = dados.length;

        // Tenta reaproveitar espaço de um bloco previamente excluído
        long endereco = getDeleted(tamDados);

        if (endereco == NULO) {
            // Não há bloco reutilizável — anexa ao final do arquivo
            endereco = arquivo.length();
            arquivo.seek(endereco);
            arquivo.writeBoolean(LAPIDE_ATIVO);
            arquivo.writeInt(tamDados);
            arquivo.write(dados);
            obj.setTamRegistro(tamDados);
        } else {
            // Bloco reutilizado: preserva o tamanho físico original do bloco
            arquivo.seek(endereco + OFF_TAM);
            int tamFisico = arquivo.readInt();

            arquivo.seek(endereco + OFF_LAPIDE);
            arquivo.writeBoolean(LAPIDE_ATIVO);

            arquivo.seek(endereco + OFF_DADOS);
            arquivo.write(dados);

            // O bloco pode ser maior que o necessário — registra o tamanho real
            obj.setTamRegistro(tamFisico);
        }

        obj.setLapide(true);
        return new CreateResult(novoID, endereco);
    }

    /**
     * Lê um registro diretamente pelo seu offset físico no arquivo.
     * Útil para acesso via índice (hash ou B+), que armazena offsets.
     *
     * @param offset posição absoluta no arquivo
     * @return objeto desserializado, ou null se o offset for inválido ou o registro estiver excluído
     */
    public T readByOffset(long offset) throws Exception {
        if (offset == NULO || offset < TAM_CABECALHO) return null;

        arquivo.seek(offset);
        boolean lapide = arquivo.readBoolean();

        // Lê o tamanho e os dados independentemente da lápide,
        // para manter o ponteiro do arquivo consistente
        int    tam  = arquivo.readInt();
        byte[] dados = new byte[tam];
        arquivo.readFully(dados);

        // Descarta registros marcados como excluídos
        if (lapide != LAPIDE_ATIVO) return null;

        T obj = construtor.newInstance();
        obj.fromByteArray(dados);
        obj.setLapide(true);
        obj.setTamRegistro(tam);
        return obj;
    }

    /**
     * Busca linear por ID no arquivo inteiro.
     * Utilizado na Fase 1 (sem índice); nas fases seguintes será substituído
     * pela busca via Hash Extensível ou Árvore B+.
     *
     * @param id identificador do registro procurado
     * @return objeto encontrado, ou null se não existir
     */
    public T read(int id) throws Exception {
        arquivo.seek(TAM_CABECALHO);

        while (arquivo.getFilePointer() < arquivo.length()) {
            boolean lapide = arquivo.readBoolean();
            int     tam    = arquivo.readInt();
            byte[]  dados  = new byte[tam];
            arquivo.readFully(dados);

            if (lapide == LAPIDE_ATIVO) {
                T obj = construtor.newInstance();
                obj.fromByteArray(dados);
                if (obj.getId() == id) {
                    obj.setLapide(true);
                    obj.setTamRegistro(tam);
                    return obj;
                }
            }
            // Registros excluídos são ignorados, mas seus bytes já foram consumidos acima
        }
        return null;
    }

    /**
     * Atualiza um registro existente (identificado por novoObj.getId()).
     *
     * Se os novos dados couberem no bloco atual, sobrescreve no lugar.
     * Se os novos dados forem maiores, marca o bloco atual como excluído,
     * devolve-o à lista de espaços livres e insere o registro em um novo local.
     *
     * Nota: total_registros não é alterado — o registro continua existindo,
     * apenas muda de posição física quando necessário.
     *
     * @return true se o registro foi encontrado e atualizado; false caso contrário
     */
    public boolean update(T novoObj) throws Exception {
        arquivo.seek(TAM_CABECALHO);

        while (arquivo.getFilePointer() < arquivo.length()) {
            long    pos       = arquivo.getFilePointer();
            boolean lapide    = arquivo.readBoolean();
            int     tamFisico = arquivo.readInt();
            byte[]  dados     = new byte[tamFisico];
            arquivo.readFully(dados);

            if (lapide == LAPIDE_ATIVO) {
                T obj = construtor.newInstance();
                obj.fromByteArray(dados);

                if (obj.getId() == novoObj.getId()) {
                    byte[] novosDados = novoObj.toByteArray();
                    int    novoTam    = novosDados.length;

                    if (novoTam <= tamFisico) {
                        // Cabe no bloco atual — sobrescreve os dados diretamente
                        arquivo.seek(pos + OFF_DADOS);
                        arquivo.write(novosDados);
                        novoObj.setLapide(true);
                        novoObj.setTamRegistro(tamFisico);
                    } else {
                        // Não cabe — devolve o bloco atual à lista de excluídos
                        arquivo.seek(pos + OFF_LAPIDE);
                        arquivo.writeBoolean(LAPIDE_EXCLUIDO);
                        addDeleted(tamFisico, pos);

                        // Tenta reutilizar outro bloco excluído suficientemente grande
                        long novoEndereco = getDeleted(novoTam);

                        if (novoEndereco == NULO) {
                            // Nenhum bloco disponível — acrescenta ao final
                            novoEndereco = arquivo.length();
                            arquivo.seek(novoEndereco);
                            arquivo.writeBoolean(LAPIDE_ATIVO);
                            arquivo.writeInt(novoTam);
                            arquivo.write(novosDados);
                            novoObj.setTamRegistro(novoTam);
                        } else {
                            // Reutiliza bloco existente, preservando seu tamanho físico
                            arquivo.seek(novoEndereco + OFF_TAM);
                            int novoTamFisico = arquivo.readInt();

                            arquivo.seek(novoEndereco + OFF_LAPIDE);
                            arquivo.writeBoolean(LAPIDE_ATIVO);

                            arquivo.seek(novoEndereco + OFF_DADOS);
                            arquivo.write(novosDados);

                            novoObj.setTamRegistro(novoTamFisico);
                        }
                        novoObj.setLapide(true);
                    }
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Exclui logicamente um registro pelo ID, marcando sua lápide como false
     * e reinserindo o bloco na lista de espaços livres (best-fit).
     *
     * @return true se encontrado e excluído; false se o ID não existir
     */
    public boolean delete(int id) throws Exception {
        arquivo.seek(TAM_CABECALHO);

        while (arquivo.getFilePointer() < arquivo.length()) {
            long    pos    = arquivo.getFilePointer();
            boolean lapide = arquivo.readBoolean();
            int     tam    = arquivo.readInt();
            byte[]  dados  = new byte[tam];
            arquivo.readFully(dados);

            if (lapide == LAPIDE_ATIVO) {
                T obj = construtor.newInstance();
                obj.fromByteArray(dados);

                if (obj.getId() == id) {
                    // Marca a lápide como excluído e devolve o bloco à lista livre
                    arquivo.seek(pos + OFF_LAPIDE);
                    arquivo.writeBoolean(LAPIDE_EXCLUIDO);
                    incrementaTotalRegistros(-1);
                    addDeleted(tam, pos);
                    return true;
                }
            }
        }
        return false;
    }

    // ------------------------------------------------------------------
    // Métodos auxiliares de cabeçalho
    // ------------------------------------------------------------------

    /** Retorna o último ID gerado (não necessariamente o maior ID ativo). */
    public int getUltimoId() throws Exception {
        arquivo.seek(OFFSET_ULTIMO_ID);
        return arquivo.readInt();
    }

    /** Retorna o número de registros ativos (não excluídos) no arquivo. */
    public int getTotalRegistros() throws Exception {
        arquivo.seek(OFFSET_TOTAL_REG);
        return arquivo.readInt();
    }

    /**
     * Incrementa ou decrementa o contador de registros ativos no cabeçalho.
     *
     * @param delta +1 para inserção, -1 para exclusão
     */
    private void incrementaTotalRegistros(int delta) throws Exception {
        arquivo.seek(OFFSET_TOTAL_REG);
        int total = arquivo.readInt();
        arquivo.seek(OFFSET_TOTAL_REG);
        arquivo.writeInt(total + delta);
    }

    // ------------------------------------------------------------------
    // Gerenciamento da lista de excluídos (best-fit ordenado por tamanho)
    // ------------------------------------------------------------------

    /**
     * Insere um bloco recém-excluído na lista encadeada de espaços livres.
     * A lista é mantida ordenada em ordem crescente de tamanho (best-fit).
     *
     * O ponteiro para o próximo bloco da lista é gravado nos primeiros
     * 8 bytes da área de dados do registro excluído.
     *
     * @param tamBloco tamanho físico do bloco excluído
     * @param posBloco offset absoluto do bloco excluído no arquivo
     */
    private void addDeleted(int tamBloco, long posBloco) throws Exception {
        arquivo.seek(OFFSET_LISTA_EXC);
        long posAnterior = NULO;
        long atual       = arquivo.readLong();

        // Percorre a lista até encontrar um bloco maior que o que será inserido
        while (atual != NULO) {
            arquivo.seek(atual + OFF_TAM);
            int tamAtual = arquivo.readInt();

            if (tamAtual > tamBloco) break; // posição correta encontrada

            posAnterior = atual;
            arquivo.seek(atual + OFF_PROX_EXCLUIDO);
            atual = arquivo.readLong();
        }

        // Encadeia o novo bloco apontando para o próximo da lista
        arquivo.seek(posBloco + OFF_PROX_EXCLUIDO);
        arquivo.writeLong(atual);

        // Atualiza o ponteiro do anterior (ou do cabeçalho) para o novo bloco
        if (posAnterior == NULO) {
            arquivo.seek(OFFSET_LISTA_EXC);
        } else {
            arquivo.seek(posAnterior + OFF_PROX_EXCLUIDO);
        }
        arquivo.writeLong(posBloco);
    }

    /**
     * Busca e remove da lista o menor bloco excluído que comporte
     * {@code tamanhoNecessario} bytes (estratégia best-fit).
     *
     * Como a lista está ordenada por tamanho crescente, o primeiro bloco
     * com tamanho >= tamanhoNecessario já é o melhor candidato.
     *
     * @param tamanhoNecessario tamanho mínimo em bytes que o bloco deve ter
     * @return offset do bloco encontrado, ou NULO se nenhum for adequado
     */
    private long getDeleted(int tamanhoNecessario) throws Exception {
        arquivo.seek(OFFSET_LISTA_EXC);
        long posAnterior = NULO;
        long atual       = arquivo.readLong();

        while (atual != NULO) {
            arquivo.seek(atual + OFF_TAM);
            int tamAtual = arquivo.readInt();

            arquivo.seek(atual + OFF_PROX_EXCLUIDO);
            long proximo = arquivo.readLong();

            if (tamAtual >= tamanhoNecessario) {
                // Remove o bloco da lista encadeada
                if (posAnterior == NULO) {
                    arquivo.seek(OFFSET_LISTA_EXC);
                } else {
                    arquivo.seek(posAnterior + OFF_PROX_EXCLUIDO);
                }
                arquivo.writeLong(proximo);
                return atual;
            }

            posAnterior = atual;
            atual       = proximo;
        }

        return NULO; // Nenhum bloco adequado encontrado
    }

    // ------------------------------------------------------------------
    // Listagem e encerramento
    // ------------------------------------------------------------------

    /**
     * Retorna todos os registros ativos do arquivo em ordem de inserção física.
     * Utilizado na Fase 1 e como fonte para a ordenação externa por intercalação.
     * A listagem ordenada por atributo será feita via travessia da Árvore B+ (Fase 2+).
     */
    public List<T> listarTodos() throws Exception {
        List<T> lista = new ArrayList<>();
        arquivo.seek(TAM_CABECALHO);

        while (arquivo.getFilePointer() < arquivo.length()) {
            boolean lapide = arquivo.readBoolean();
            int     tam    = arquivo.readInt();
            byte[]  dados  = new byte[tam];
            arquivo.readFully(dados);

            if (lapide == LAPIDE_ATIVO) {
                T obj = construtor.newInstance();
                obj.fromByteArray(dados);
                obj.setLapide(true);
                obj.setTamRegistro(tam);
                lista.add(obj);
            }
            // Blocos excluídos são pulados — seus bytes já foram consumidos acima
        }
        return lista;
    }

    // ------------------------------------------------------------------
    // Suporte a índices — Fases 2 e 3
    // ------------------------------------------------------------------

    /**
     * Par (objeto, offset físico) retornado por {@link #listarComOffset()}.
     * Usado pela ordenação externa por intercalação e pela carga inicial do Hash.
     */
    public static class OffsetEntry<T> {
        public final T    objeto;
        public final long offset;

        public OffsetEntry(T objeto, long offset) {
            this.objeto = objeto;
            this.offset = offset;
        }
    }

    /**
     * Varre o arquivo e retorna cada registro ativo junto com seu offset físico.
     *
     * Uso principal:
     * - Carga inicial do Hash Extensível: popula id → offset para todos os registros.
     * - Ordenação externa por intercalação: precisa do offset para reescrever blocos
     *   ou para que a B+ aponte diretamente para a posição correta no .bin.
     */
    public List<OffsetEntry<T>> listarComOffset() throws Exception {
        List<OffsetEntry<T>> lista = new ArrayList<>();
        arquivo.seek(TAM_CABECALHO);

        while (arquivo.getFilePointer() < arquivo.length()) {
            long    pos    = arquivo.getFilePointer(); // offset do início deste registro
            boolean lapide = arquivo.readBoolean();
            int     tam    = arquivo.readInt();
            byte[]  dados  = new byte[tam];
            arquivo.readFully(dados);

            if (lapide == LAPIDE_ATIVO) {
                T obj = construtor.newInstance();
                obj.fromByteArray(dados);
                ((Registro) obj).setLapide(true);
                ((Registro) obj).setTamRegistro(tam);
                lista.add(new OffsetEntry<>(obj, pos));
            }
        }
        return lista;
    }

    /**
     * Retorna o offset físico de um registro pelo seu ID, via varredura linear.
     *
     * Utilizado uma única vez para popular o Hash Extensível na primeira carga.
     * Após isso, todas as buscas por ID devem passar pelo hash (O(1) amortizado).
     *
     * @param id identificador do registro
     * @return offset absoluto no arquivo, ou NULO se o ID não existir
     */
    public long getOffsetById(int id) throws Exception {
        arquivo.seek(TAM_CABECALHO);

        while (arquivo.getFilePointer() < arquivo.length()) {
            long    pos    = arquivo.getFilePointer();
            boolean lapide = arquivo.readBoolean();
            int     tam    = arquivo.readInt();
            byte[]  dados  = new byte[tam];
            arquivo.readFully(dados);

            if (lapide == LAPIDE_ATIVO) {
                T obj = construtor.newInstance();
                obj.fromByteArray(dados);
                if (((Registro) obj).getId() == id) return pos;
            }
        }
        return NULO;
    }

    /**
     * Atualiza um registro diretamente pelo seu offset físico, sem varredura.
     *
     * Chamado quando o Hash Extensível ou a Árvore B+ já forneceu o offset exato,
     * evitando percorrer o arquivo inteiro.
     *
     * Regras de reaproveitamento são as mesmas do {@link #update}:
     * - Se caber no bloco atual, sobrescreve no lugar.
     * - Se não couber, marca o bloco como excluído e realoca em outro lugar.
     *   Nesse caso retorna o novo offset para que o índice possa ser atualizado.
     *
     * @param offset  offset físico atual do registro
     * @param novoObj objeto com os dados atualizados (deve ter o mesmo ID)
     * @return offset físico final do registro (pode ser diferente do original se realocado)
     */
    public long updateByOffset(long offset, T novoObj) throws Exception {
        if (offset == NULO || offset < TAM_CABECALHO) return NULO;

        arquivo.seek(offset + OFF_TAM);
        int tamFisico = arquivo.readInt();

        byte[] novosDados = novoObj.toByteArray();
        int    novoTam    = novosDados.length;

        if (novoTam <= tamFisico) {
            // Cabe no bloco atual — sobrescreve sem mover o registro
            arquivo.seek(offset + OFF_DADOS);
            arquivo.write(novosDados);
            novoObj.setLapide(true);
            novoObj.setTamRegistro(tamFisico);
            return offset; // offset não mudou
        }

        // Não cabe — libera o bloco atual e realoca
        arquivo.seek(offset + OFF_LAPIDE);
        arquivo.writeBoolean(LAPIDE_EXCLUIDO);
        addDeleted(tamFisico, offset);

        long novoOffset = getDeleted(novoTam);

        if (novoOffset == NULO) {
            // Acrescenta ao final do arquivo
            novoOffset = arquivo.length();
            arquivo.seek(novoOffset);
            arquivo.writeBoolean(LAPIDE_ATIVO);
            arquivo.writeInt(novoTam);
            arquivo.write(novosDados);
            novoObj.setTamRegistro(novoTam);
        } else {
            arquivo.seek(novoOffset + OFF_TAM);
            int novoTamFisico = arquivo.readInt();

            arquivo.seek(novoOffset + OFF_LAPIDE);
            arquivo.writeBoolean(LAPIDE_ATIVO);

            arquivo.seek(novoOffset + OFF_DADOS);
            arquivo.write(novosDados);

            novoObj.setTamRegistro(novoTamFisico);
        }

        novoObj.setLapide(true);
        // O índice (hash ou B+) deve ser atualizado para apontar para novoOffset
        return novoOffset;
    }

    /**
     * Exclui logicamente um registro diretamente pelo seu offset físico.
     *
     * Chamado quando o Hash Extensível ou a Árvore B+ já localizou o bloco,
     * tornando desnecessária a varredura que o {@link #delete(int)} faz.
     *
     * @param offset offset físico do registro a excluir
     * @param tam    tamanho físico do bloco (lido pelo índice ou passado pelo chamador)
     */
    public void deleteByOffset(long offset, int tam) throws Exception {
        if (offset == NULO || offset < TAM_CABECALHO) return;

        arquivo.seek(offset + OFF_LAPIDE);
        boolean lapide = arquivo.readBoolean();

        // Só exclui se o registro ainda estiver ativo (proteção contra dupla exclusão)
        if (lapide != LAPIDE_ATIVO) return;

        arquivo.seek(offset + OFF_LAPIDE);
        arquivo.writeBoolean(LAPIDE_EXCLUIDO);
        incrementaTotalRegistros(-1);
        addDeleted(tam, offset);
    }

    /** Fecha o arquivo liberando o descritor de I/O. */
    public void close() throws Exception {
        arquivo.close();
    }
}