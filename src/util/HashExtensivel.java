package src.util;

import java.io.File;
import java.io.RandomAccessFile;
import java.util.ArrayList;

public class HashExtensivel {
    private String arqDiretorio;
    private String arqCestos;
    private int maxDadosPorCesto;

    public HashExtensivel(int maxDadosPorCesto, String arqDiretorio, String arqCestos) throws Exception {
        this.maxDadosPorCesto = maxDadosPorCesto;
        this.arqDiretorio = "./data/" + arqDiretorio;
        this.arqCestos = "./data/" + arqCestos;

        File d = new File("./data");
        if (!d.exists()) d.mkdir();

        File dir = new File(this.arqDiretorio);
        if (!dir.exists()) {
            // Inicializa Diretório com Profundidade Global 0
            RandomAccessFile rafDir = new RandomAccessFile(this.arqDiretorio, "rw");
            rafDir.writeByte(0); // Profundidade Global
            
            // Inicializa o primeiro Cesto (Profundidade Local 0)
            Cesto cestoVazio = new Cesto((byte)0);
            long enderecoCesto = gravarNovoCesto(cestoVazio);
            
            rafDir.writeLong(enderecoCesto); // Ponteiro para o cesto 0
            rafDir.close();
        }
    }

    private int funcaoHash(String chave) {
        // CORREÇÃO: Math.abs(Integer.MIN_VALUE) retorna MIN_VALUE (negativo),
        // o que causaria ArrayIndexOutOfBoundsException.
        // O operador & 0x7FFFFFFF zera o bit de sinal, sempre retornando >= 0.
        return chave.hashCode() & 0x7FFFFFFF;
    }

    public boolean create(String chave, int valor) throws Exception {
        RandomAccessFile rafDir = new RandomAccessFile(arqDiretorio, "rw");
        byte profGlobal = rafDir.readByte();
        int hash = funcaoHash(chave);
        
        // Calcula o índice no diretório pegando os 'p' últimos bits do hash
        int indiceDir = hash & ((1 << profGlobal) - 1); 
        rafDir.seek(1 + (indiceDir * 8));
        long enderecoCesto = rafDir.readLong();
        rafDir.close();

        Cesto cesto = lerCesto(enderecoCesto);

        // Verifica se a chave já existe
        for (int i = 0; i < cesto.quantidade; i++) {
            if (cesto.chaves[i].equals(chave)) return false; 
        }

        // Se tem espaço, insere no cesto atual
        if (cesto.quantidade < maxDadosPorCesto) {
            cesto.chaves[cesto.quantidade] = chave;
            cesto.valores[cesto.quantidade] = valor;
            cesto.quantidade++;
            atualizarCesto(cesto, enderecoCesto);
            return true;
        }

        // Se o cesto está cheio, faz o SPLIT
        if (cesto.profLocal >= profGlobal) {
            duplicarDiretorio();
            profGlobal++; // Aumentou a profundidade global
        }

        // Divide o cesto
        Cesto novoCesto = new Cesto((byte)(cesto.profLocal + 1));
        cesto.profLocal++;
        
        ArrayList<String> chavesTemp = new ArrayList<>();
        ArrayList<Integer> valoresTemp = new ArrayList<>();
        for(int i = 0; i < cesto.quantidade; i++) {
            chavesTemp.add(cesto.chaves[i]);
            valoresTemp.add(cesto.valores[i]);
        }
        chavesTemp.add(chave);
        valoresTemp.add(valor);

        cesto.quantidade = 0; // Limpa o cesto antigo para redistribuir
        long enderecoNovoCesto = gravarNovoCesto(novoCesto);

        // Redistribui as chaves entre o cesto velho e o novo
        for (int i = 0; i < chavesTemp.size(); i++) {
            String c = chavesTemp.get(i);
            int v = valoresTemp.get(i);
            int h = funcaoHash(c);
            int novoIndice = h & ((1 << cesto.profLocal) - 1);

            // Verifica qual bit mudou para decidir para qual cesto vai
            if ((novoIndice & (1 << (cesto.profLocal - 1))) == 0) {
                cesto.chaves[cesto.quantidade] = c;
                cesto.valores[cesto.quantidade] = v;
                cesto.quantidade++;
            } else {
                novoCesto.chaves[novoCesto.quantidade] = c;
                novoCesto.valores[novoCesto.quantidade] = v;
                novoCesto.quantidade++;
            }
        }

        atualizarCesto(cesto, enderecoCesto);
        atualizarCesto(novoCesto, enderecoNovoCesto);
        atualizarPonteirosDiretorio(hash, cesto.profLocal, enderecoCesto, enderecoNovoCesto);

        return true; // Sucesso após split
    }

    public int read(String chave) throws Exception {
        RandomAccessFile rafDir = new RandomAccessFile(arqDiretorio, "r");
        byte profGlobal = rafDir.readByte();
        int hash = funcaoHash(chave);
        int indiceDir = hash & ((1 << profGlobal) - 1);
        
        rafDir.seek(1 + (indiceDir * 8));
        long enderecoCesto = rafDir.readLong();
        rafDir.close();

        Cesto cesto = lerCesto(enderecoCesto);
        for (int i = 0; i < cesto.quantidade; i++) {
            if (cesto.chaves[i].equals(chave)) return cesto.valores[i];
        }
        return -1;
    }

    public boolean update(String chave, int novoValor) throws Exception {
        RandomAccessFile rafDir = new RandomAccessFile(arqDiretorio, "r");
        byte profGlobal = rafDir.readByte();
        int hash = funcaoHash(chave);
        int indiceDir = hash & ((1 << profGlobal) - 1);
        
        rafDir.seek(1 + (indiceDir * 8));
        long enderecoCesto = rafDir.readLong();
        rafDir.close();

        Cesto cesto = lerCesto(enderecoCesto);
        for (int i = 0; i < cesto.quantidade; i++) {
            if (cesto.chaves[i].equals(chave)) {
                cesto.valores[i] = novoValor;
                atualizarCesto(cesto, enderecoCesto);
                return true;
            }
        }
        return false;
    }

    public boolean delete(String chave) throws Exception {
        RandomAccessFile rafDir = new RandomAccessFile(arqDiretorio, "r");
        byte profGlobal = rafDir.readByte();
        int hash = funcaoHash(chave);
        int indiceDir = hash & ((1 << profGlobal) - 1);
        
        rafDir.seek(1 + (indiceDir * 8));
        long enderecoCesto = rafDir.readLong();
        rafDir.close();

        Cesto cesto = lerCesto(enderecoCesto);
        for (int i = 0; i < cesto.quantidade; i++) {
            if (cesto.chaves[i].equals(chave)) {
                // Remove movendo o último elemento para a posição apagada
                cesto.chaves[i] = cesto.chaves[cesto.quantidade - 1];
                cesto.valores[i] = cesto.valores[cesto.quantidade - 1];
                cesto.quantidade--;
                atualizarCesto(cesto, enderecoCesto);
                return true;
            }
        }
        return false;
    }

    // --- MÉTODOS AUXILIARES DE DISCO ---

    private void duplicarDiretorio() throws Exception {
        RandomAccessFile rafDir = new RandomAccessFile(arqDiretorio, "rw");
        byte profGlobal = rafDir.readByte();
        int tamanhoAtual = 1 << profGlobal; // 2^profGlobal
        
        rafDir.seek(1);
        long[] ponteiros = new long[tamanhoAtual];
        for(int i=0; i<tamanhoAtual; i++) ponteiros[i] = rafDir.readLong();
        
        rafDir.seek(1 + (tamanhoAtual * 8));
        for(int i=0; i<tamanhoAtual; i++) rafDir.writeLong(ponteiros[i]); // Duplica os ponteiros
        
        rafDir.seek(0);
        rafDir.writeByte(profGlobal + 1); // Atualiza profundidade global
        rafDir.close();
    }

    private void atualizarPonteirosDiretorio(int hashOriginal, byte novaProfLocal, long endAntigo, long endNovo) throws Exception {
        RandomAccessFile rafDir = new RandomAccessFile(arqDiretorio, "rw");
        byte profGlobal = rafDir.readByte();
        int totalPonteiros = 1 << profGlobal;
        
        // O bit que causou a divisão
        int bitDivisao = 1 << (novaProfLocal - 1);
        int padraoAntigo = hashOriginal & ((1 << (novaProfLocal - 1)) - 1);

        for (int i = 0; i < totalPonteiros; i++) {
            // Se o índice partilha o mesmo sufixo do cesto que dividiu
            if ((i & ((1 << (novaProfLocal - 1)) - 1)) == padraoAntigo) {
                rafDir.seek(1 + (i * 8));
                if ((i & bitDivisao) == 0) {
                    rafDir.writeLong(endAntigo);
                } else {
                    rafDir.writeLong(endNovo);
                }
            }
        }
        rafDir.close();
    }

    private class Cesto {
        byte profLocal;
        int quantidade;
        String[] chaves;
        int[] valores;

        public Cesto(byte profLocal) {
            this.profLocal = profLocal;
            this.quantidade = 0;
            this.chaves = new String[maxDadosPorCesto];
            this.valores = new int[maxDadosPorCesto];
        }
    }

    private long gravarNovoCesto(Cesto cesto) throws Exception {
        RandomAccessFile rafCestos = new RandomAccessFile(arqCestos, "rw");
        long endereco = rafCestos.length();
        atualizarCestoNoArquivo(rafCestos, endereco, cesto);
        rafCestos.close();
        return endereco;
    }

    private void atualizarCesto(Cesto cesto, long endereco) throws Exception {
        RandomAccessFile rafCestos = new RandomAccessFile(arqCestos, "rw");
        atualizarCestoNoArquivo(rafCestos, endereco, cesto);
        rafCestos.close();
    }

   private void atualizarCestoNoArquivo(RandomAccessFile raf, long endereco, Cesto cesto) throws Exception {
        raf.seek(endereco);
        raf.writeByte(cesto.profLocal);
        raf.writeInt(cesto.quantidade);
        for (int i = 0; i < maxDadosPorCesto; i++) {
            String chave = (i < cesto.quantidade) ? cesto.chaves[i] : "";
            raf.writeUTF(chave); // <-- SUBSTITUÍDO AQUI
            int valor = (i < cesto.quantidade) ? cesto.valores[i] : -1;
            raf.writeInt(valor);
        }
    }

    private Cesto lerCesto(long endereco) throws Exception {
        RandomAccessFile raf = new RandomAccessFile(arqCestos, "r");
        raf.seek(endereco);
        Cesto cesto = new Cesto(raf.readByte());
        cesto.quantidade = raf.readInt();
        for (int i = 0; i < maxDadosPorCesto; i++) {
            cesto.chaves[i] = raf.readUTF(); // <-- SUBSTITUÍDO AQUI
            cesto.valores[i] = raf.readInt();
        }
        raf.close();
        return cesto;
    }
}