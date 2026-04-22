package src.util;

import java.io.File;
import java.io.RandomAccessFile;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;


public class Arquivo<T extends Registro> {

    // Constantes — offsets fixos do cabeçalho
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

    // Construtor

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

    // Classe auxiliar para retornar id + endereço físico após criação

    public static class CreateResult {
        public final int  id;
        public final long endereco;

        CreateResult(int id, long endereco) {
            this.id       = id;
            this.endereco = endereco;
        }
    }

    // CRUD

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

    // Métodos auxiliares de cabeçalho

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

    // Gerenciamento da lista de excluídos (best-fit ordenado por tamanho)

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

    // Listagem e encerramento

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

    // Suporte a índices 

    public static class OffsetEntry<T> {
        public final T    objeto;
        public final long offset;

        public OffsetEntry(T objeto, long offset) {
            this.objeto = objeto;
            this.offset = offset;
        }
    }

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