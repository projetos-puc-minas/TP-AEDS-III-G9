package src.util;

import java.io.File;
import java.io.RandomAccessFile;
import java.lang.reflect.Constructor;

public class Arquivo<T extends Registro> {

    private static final int TAM_CABECALHO = 16;
    private static final int OFFSET_ULTIMO_ID = 0;
    private static final int OFFSET_TOTAL_REG = 4;
    private static final int OFFSET_LISTA_EXC = 8;

    private static final boolean LAPIDE_ATIVO = true;
    private static final boolean LAPIDE_EXCLUIDO = false;

    private static final long NULO = -1;

    private RandomAccessFile arquivo;
    private Constructor<T> construtor;

    public Arquivo(String nomeArquivo, Constructor<T> construtor) throws Exception {
        File diretorio = new File("./data");
        if (!diretorio.exists()) diretorio.mkdir();

        diretorio = new File("./data/" + nomeArquivo);
        if (!diretorio.exists()) diretorio.mkdir();

        this.construtor = construtor;
        this.arquivo = new RandomAccessFile("./data/" + nomeArquivo + ".bin", "rw");

        if (arquivo.length() < TAM_CABECALHO) {
            arquivo.seek(0);
            arquivo.writeInt(0);
            arquivo.writeInt(0);
            arquivo.writeLong(NULO);
        }
    }

    // --- crud ---

    public int create(T obj) throws Exception {
        arquivo.seek(OFFSET_ULTIMO_ID);
        int novoID = arquivo.readInt() + 1;
        arquivo.seek(OFFSET_ULTIMO_ID);
        arquivo.writeInt(novoID);
        incrementaTotalRegistros(1);

        obj.setId(novoID);
        byte[] dados = obj.toByteArray();
        int tamanho = dados.length;

        long endereco = getDeleted(tamanho);

        if (endereco == NULO) {
            arquivo.seek(arquivo.length());
            arquivo.writeBoolean(LAPIDE_ATIVO);
            arquivo.writeInt(tamanho);
            arquivo.write(dados);
        } else {
            arquivo.seek(endereco);
            arquivo.writeBoolean(LAPIDE_ATIVO);
            arquivo.skipBytes(4);
            arquivo.write(dados);
        }

        obj.setLapide(true);
        obj.setTamRegistro(tamanho);
        return novoID;
    }

    public T read(int id) throws Exception {
        arquivo.seek(TAM_CABECALHO);

        while (arquivo.getFilePointer() < arquivo.length()) {
            boolean lapide = arquivo.readBoolean();
            int tamanho  = arquivo.readInt();
            byte[] dados = new byte[tamanho];
            arquivo.readFully(dados);

            if (lapide == LAPIDE_ATIVO) {
                T obj = construtor.newInstance();
                obj.fromByteArray(dados);
                if (obj.getId() == id) {
                    obj.setLapide(true);
                    obj.setTamRegistro(tamanho);
                    return obj;
                }
            }
        }
        return null;
    }

    public boolean delete(int id) throws Exception {
        arquivo.seek(TAM_CABECALHO);

        while (arquivo.getFilePointer() < arquivo.length()) {
            long posicao   = arquivo.getFilePointer();
            boolean lapide = arquivo.readBoolean();
            int tamanho = arquivo.readInt();
            byte[] dados = new byte[tamanho];
            arquivo.readFully(dados);

            if (lapide == LAPIDE_ATIVO) {
                T obj = construtor.newInstance();
                obj.fromByteArray(dados);
                if (obj.getId() == id) {
                    arquivo.seek(posicao);
                    arquivo.writeBoolean(LAPIDE_EXCLUIDO);
                    incrementaTotalRegistros(-1);
                    addDeleted(tamanho, posicao);
                    return true;
                }
            }
        }
        return false;
    }

    public boolean update(T novoObj) throws Exception {
        arquivo.seek(TAM_CABECALHO);

        while (arquivo.getFilePointer() < arquivo.length()) {
            long posicao     = arquivo.getFilePointer();
            boolean lapide   = arquivo.readBoolean();
            int tamanhoAtual = arquivo.readInt();
            byte[] dados     = new byte[tamanhoAtual];
            arquivo.readFully(dados);

            if (lapide == LAPIDE_ATIVO) {
                T obj = construtor.newInstance();
                obj.fromByteArray(dados);

                if (obj.getId() == novoObj.getId()) {
                    byte[] novosDados = novoObj.toByteArray();
                    int novoTamanho = novosDados.length;

                    if (novoTamanho <= tamanhoAtual) {
                        arquivo.seek(posicao + 1 + 4);
                        arquivo.write(novosDados);
                        novoObj.setLapide(true);
                        novoObj.setTamRegistro(tamanhoAtual);
                    } else {
                        arquivo.seek(posicao);
                        arquivo.writeBoolean(LAPIDE_EXCLUIDO);
                        addDeleted(tamanhoAtual, posicao);

                        long novoEndereco = getDeleted(novoTamanho);
                        if (novoEndereco == NULO) {
                            arquivo.seek(arquivo.length());
                            arquivo.writeBoolean(LAPIDE_ATIVO);
                            arquivo.writeInt(novoTamanho);
                            arquivo.write(novosDados);
                        } else {
                            arquivo.seek(novoEndereco);
                            arquivo.writeBoolean(LAPIDE_ATIVO);
                            arquivo.skipBytes(4);
                            arquivo.write(novosDados);
                        }
                        novoObj.setLapide(true);
                        novoObj.setTamRegistro(novoTamanho);
                    }
                    return true;
                }
            }
        }
        return false;
    }

    // --- cabeçalho ---

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

    // --- lista de excluídos ---

    private void addDeleted(int tamanhoEspaco, long enderecoEspaco) throws Exception {
        arquivo.seek(OFFSET_LISTA_EXC);
        long anterior = OFFSET_LISTA_EXC;
        long atual = arquivo.readLong();

        while (atual != NULO) {
            arquivo.seek(atual + 1);
            int tamAtual  = arquivo.readInt();
            long proximo  = arquivo.readLong();

            if (tamAtual > tamanhoEspaco) {
                break;
            }

            anterior = atual + 1 + 4;
            atual = proximo;
        }

        arquivo.seek(enderecoEspaco + 1 + 4);
        arquivo.writeLong(atual);

        if (anterior == OFFSET_LISTA_EXC) {
            arquivo.seek(OFFSET_LISTA_EXC);
        } else {
            arquivo.seek(anterior);
        }
        arquivo.writeLong(enderecoEspaco);
    }

    private long getDeleted(int tamanhoNecessario) throws Exception {
        arquivo.seek(OFFSET_LISTA_EXC);
        long anterior = OFFSET_LISTA_EXC;
        long atual    = arquivo.readLong();

        while (atual != NULO) {
            arquivo.seek(atual + 1);
            int tamAtual = arquivo.readInt();
            long proximo = arquivo.readLong();

            if (tamAtual >= tamanhoNecessario) {
                if (anterior == OFFSET_LISTA_EXC) {
                    arquivo.seek(OFFSET_LISTA_EXC);
                } else {
                    arquivo.seek(anterior);
                }
                arquivo.writeLong(proximo);
                return atual;
            }

            anterior = atual + 1 + 4;
            atual = proximo;
        }
        return NULO;
    }

    // --- fechamento ---

    public void close() throws Exception {
        arquivo.close();
    }
}