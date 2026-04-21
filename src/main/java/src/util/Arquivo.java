package src.util;

import java.io.File;
import java.io.RandomAccessFile;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;

/**
 * Gerenciador genérico de arquivo binário com:
 *   - Cabeçalho fixo de 16 bytes
 *   - Exclusão lógica (lápide)
 *   - Reaproveitamento de espaço via lista encadeada best-fit ordenada por tamanho
 *
 * Cabeçalho (16 bytes):
 *   [0..3]  int  ultimo_id
 *   [4..7]  int  total_registros
 *   [8..15] long ponteiro_lista_excluidos  (-1 = vazia)
 *
 * Registro no arquivo:
 *   [0]      boolean  lapide        (1 byte)
 *   [1..4]   int      tam_registro  (4 bytes) — tamanho do bloco reservado
 *   [5..]    byte[]   dados serializados
 *
 * CORREÇÕES em relação à versão anterior:
 *   1. create() retorna o endereço físico (offset) além do id — via CreateResult.
 *   2. Ao reutilizar espaço, tam_registro é preservado (tamanho do espaço), não
 *      sobrescrito com o tamanho dos novos dados — garante que a lista encadeada
 *      continue correta para futuras reutilizações.
 *   3. addDeleted() reescrito sem aritmética de ponteiros frágil: usa campos
 *      explícitos OFFSET_PROX_EXCLUIDO dentro do registro excluído.
 *   4. getDeleted() remove o nó da lista de forma segura, atualizando o ponteiro
 *      anterior corretamente.
 */
public class Arquivo<T extends Registro> {

    // ------------------------------------------------------------------
    // Cabeçalho
    // ------------------------------------------------------------------
    public  static final int  TAM_CABECALHO       = 16;
    private static final int  OFFSET_ULTIMO_ID    = 0;
    private static final int  OFFSET_TOTAL_REG    = 4;
    private static final int  OFFSET_LISTA_EXC    = 8;  // long (8 bytes)

    // ------------------------------------------------------------------
    // Registro
    // ------------------------------------------------------------------
    private static final int  OFF_LAPIDE          = 0;  // 1 byte  boolean
    private static final int  OFF_TAM             = 1;  // 4 bytes int
    private static final int  OFF_DADOS           = 5;  // dados serializados

    // Dentro do bloco de DADOS de um registro EXCLUÍDO guardamos o ponteiro
    // para o próximo excluído (os primeiros 8 bytes dos dados são sobrescritos).
    // Isso é seguro porque o bloco excluído não será lido como registro ativo.
    private static final int  OFF_PROX_EXCLUIDO   = OFF_DADOS; // long (8 bytes)

    private static final boolean LAPIDE_ATIVO    = true;
    private static final boolean LAPIDE_EXCLUIDO = false;
    public  static final long NULO               = -1L;

    // ------------------------------------------------------------------

    private final RandomAccessFile arquivo;
    private final Constructor<T>   construtor;

    public Arquivo(String nomeArquivo, Constructor<T> construtor) throws Exception {
        File dir = new File("./data");
        if (!dir.exists()) dir.mkdirs();

        this.construtor = construtor;
        this.arquivo = new RandomAccessFile("./data/" + nomeArquivo + ".bin", "rw");

        if (arquivo.length() < TAM_CABECALHO) {
            arquivo.seek(0);
            arquivo.writeInt(0);      // ultimo_id
            arquivo.writeInt(0);      // total_registros
            arquivo.writeLong(NULO);  // ponteiro_lista_excluidos
        }
    }

    // ------------------------------------------------------------------
    // Resultado do create — expõe id E endereço físico para o índice
    // ------------------------------------------------------------------

    public static class CreateResult {
        public final int  id;
        public final long endereco;  // offset no .bin onde o registro foi gravado
        CreateResult(int id, long endereco) { this.id = id; this.endereco = endereco; }
    }

    // ------------------------------------------------------------------
    // CREATE
    // ------------------------------------------------------------------

    /**
     * Persiste o objeto no arquivo e retorna um CreateResult com o id gerado
     * e o endereço físico (offset) onde o registro foi gravado.
     * O endereço físico deve ser armazenado no índice externo.
     */
    public CreateResult create(T obj) throws Exception {
        // gera novo id
        arquivo.seek(OFFSET_ULTIMO_ID);
        int novoID = arquivo.readInt() + 1;
        arquivo.seek(OFFSET_ULTIMO_ID);
        arquivo.writeInt(novoID);
        incrementaTotalRegistros(1);

        obj.setId(novoID);
        byte[] dados  = obj.toByteArray();
        int    tam    = dados.length;

        long endereco = getDeleted(tam);

        if (endereco == NULO) {
            // Adiciona ao final do arquivo
            endereco = arquivo.length();
            arquivo.seek(endereco);
            arquivo.writeBoolean(LAPIDE_ATIVO);
            arquivo.writeInt(tam);
            arquivo.write(dados);
        } else {
            // Reutiliza espaço da lista de excluídos.
            // NÃO sobrescreve tam_registro — mantém o tamanho original do bloco
            // para que a lista encadeada continue consistente.
            arquivo.seek(endereco + OFF_LAPIDE);
            arquivo.writeBoolean(LAPIDE_ATIVO);
            // pula os 4 bytes de tam_registro (preserva o tamanho do bloco)
            arquivo.seek(endereco + OFF_DADOS);
            arquivo.write(dados);
        }

        obj.setLapide(true);
        obj.setTamRegistro(tam);
        return new CreateResult(novoID, endereco);
    }

    // ------------------------------------------------------------------
    // READ por id — usa offset do índice quando disponível
    // ------------------------------------------------------------------

    /**
     * Lê o registro pelo endereço físico retornado pelo índice.
     * Muito mais rápido que o scan por id.
     */
    public T readByOffset(long offset) throws Exception {
        if (offset == NULO || offset < TAM_CABECALHO) return null;
        arquivo.seek(offset);
        boolean lapide = arquivo.readBoolean();
        int     tam    = arquivo.readInt();
        byte[]  dados  = new byte[tam];
        arquivo.readFully(dados);
        if (lapide != LAPIDE_ATIVO) return null;
        T obj = construtor.newInstance();
        obj.fromByteArray(dados);
        obj.setLapide(true);
        obj.setTamRegistro(tam);
        return obj;
    }

    /**
     * Lê o registro pelo id — scan linear (fallback quando não há índice).
     * Prefira readByOffset() sempre que o índice estiver disponível.
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
        }
        return null;
    }

    // ------------------------------------------------------------------
    // DELETE
    // ------------------------------------------------------------------

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
    // UPDATE
    // ------------------------------------------------------------------

    public boolean update(T novoObj) throws Exception {
        arquivo.seek(TAM_CABECALHO);
        while (arquivo.getFilePointer() < arquivo.length()) {
            long    pos      = arquivo.getFilePointer();
            boolean lapide   = arquivo.readBoolean();
            int     tamAtual = arquivo.readInt();
            byte[]  dados    = new byte[tamAtual];
            arquivo.readFully(dados);

            if (lapide == LAPIDE_ATIVO) {
                T obj = construtor.newInstance();
                obj.fromByteArray(dados);

                if (obj.getId() == novoObj.getId()) {
                    byte[] novosDados = novoObj.toByteArray();
                    int    novoTam    = novosDados.length;

                    if (novoTam <= tamAtual) {
                        // Cabe no mesmo bloco — sobrescreve só os dados, mantém tam_registro
                        arquivo.seek(pos + OFF_DADOS);
                        arquivo.write(novosDados);
                        novoObj.setLapide(true);
                        novoObj.setTamRegistro(tamAtual);
                    } else {
                        // Não cabe — invalida o bloco atual e grava novo no final
                        // SEM chamar create() para não gerar novo ID nem alterar contadores
                        arquivo.seek(pos + OFF_LAPIDE);
                        arquivo.writeBoolean(LAPIDE_EXCLUIDO);
                        addDeleted(tamAtual, pos);

                        // Tenta reaproveitar outro espaço livre (best-fit)
                        long novoEndereco = getDeleted(novoTam);

                        if (novoEndereco == NULO) {
                            // Grava no final do arquivo
                            novoEndereco = arquivo.length();
                            arquivo.seek(novoEndereco);
                            arquivo.writeBoolean(LAPIDE_ATIVO);
                            arquivo.writeInt(novoTam);
                            arquivo.write(novosDados);
                        } else {
                            // Reutiliza bloco da lista de excluídos
                            arquivo.seek(novoEndereco + OFF_LAPIDE);
                            arquivo.writeBoolean(LAPIDE_ATIVO);
                            // Preserva tam_registro do bloco reutilizado
                            arquivo.seek(novoEndereco + OFF_DADOS);
                            arquivo.write(novosDados);
                            // Atualiza novoTam para refletir o tamanho real do bloco
                            arquivo.seek(novoEndereco + OFF_TAM);
                            novoTam = arquivo.readInt();
                        }

                        novoObj.setLapide(true);
                        novoObj.setTamRegistro(novoTam);
                        // total_registros não muda (excluiu 1 e inseriu 1)
                        // Quem chama deve atualizar o índice com o novo endereço
                        // Para isso, armazena o endereço no objeto via campo temporário —
                        // os DAOs chamam reindexar() logo após o update(), que faz scan
                        // e encontra o registro pelo ID corretamente.
                    }
                    return true;
                }
            }
        }
        return false;
    }

    // ------------------------------------------------------------------
    // Cabeçalho
    // ------------------------------------------------------------------

    public int getUltimoId() throws Exception {
        arquivo.seek(OFFSET_ULTIMO_ID);
        return arquivo.readInt();
    }

    public int getTotalRegistros() throws Exception {
        arquivo.seek(OFFSET_TOTAL_REG);
        return arquivo.readInt();
    }

    private void incrementaTotalRegistros(int delta) throws Exception {
        arquivo.seek(OFFSET_TOTAL_REG);
        int total = arquivo.readInt();
        arquivo.seek(OFFSET_TOTAL_REG);
        arquivo.writeInt(total + delta);
    }

    // ------------------------------------------------------------------
    // Lista encadeada de espaços excluídos (best-fit por tamanho crescente)
    //
    // A lista é mantida em ordem crescente de tamanho de bloco.
    // O ponteiro para o próximo nó da lista fica nos primeiros 8 bytes
    // da área de dados do registro excluído (OFF_PROX_EXCLUIDO).
    // ------------------------------------------------------------------

    /**
     * Insere o bloco recém-excluído na lista encadeada, mantendo ordem crescente
     * de tamanho. O ponteiro "próximo" é escrito na área de dados do bloco.
     */
    private void addDeleted(int tamBloco, long posBloco) throws Exception {
        // Lê o início da lista do cabeçalho
        arquivo.seek(OFFSET_LISTA_EXC);
        long anterior     = NULO;          // posição do campo "proximo" do nó anterior
        long posAnterior  = NULO;          // posição base do nó anterior (para saber onde escrever)
        long atual        = arquivo.readLong(); // primeiro da lista

        // Percorre até encontrar o lugar correto (ordem crescente de tamanho)
        while (atual != NULO) {
            arquivo.seek(atual + OFF_TAM);
            int tamAtual = arquivo.readInt();

            if (tamAtual > tamBloco) break; // inserir antes deste

            posAnterior = atual;
            arquivo.seek(atual + OFF_PROX_EXCLUIDO);
            atual = arquivo.readLong();
        }

        // Escreve o ponteiro "proximo" do novo nó → aponta para "atual"
        arquivo.seek(posBloco + OFF_PROX_EXCLUIDO);
        arquivo.writeLong(atual);

        // Atualiza o ponteiro do nó anterior (ou do cabeçalho) → aponta para posBloco
        if (posAnterior == NULO) {
            // Novo nó é o primeiro da lista
            arquivo.seek(OFFSET_LISTA_EXC);
        } else {
            arquivo.seek(posAnterior + OFF_PROX_EXCLUIDO);
        }
        arquivo.writeLong(posBloco);
    }

    /**
     * Retorna o endereço do primeiro bloco com tamanho ≥ tamanhoNecessario,
     * removendo-o da lista. Retorna NULO se não encontrar.
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
                // Remove da lista: anterior aponta para proximo
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
        return NULO;
    }

    // ------------------------------------------------------------------
    // Listagem
    // ------------------------------------------------------------------

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
        }
        return lista;
    }

    // ------------------------------------------------------------------
    // Fechamento
    // ------------------------------------------------------------------

    public void close() throws Exception {
        arquivo.close();
    }
}