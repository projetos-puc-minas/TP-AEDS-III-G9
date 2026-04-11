package src.util;

import java.io.File;
import java.io.RandomAccessFile;

public class ArvoreBMais implements Indexador {

    public  static final int ORDEM = 4;
    private static final int TAM_CABECALHO = 32;
    private static final long NULO = -1L;
    private static final int OFFSET_RAIZ = 0;
    private static final int OFFSET_TOTAL = 8;

    private final RandomAccessFile idx;

    public ArvoreBMais(String nomeBase) throws Exception {
        File dir = new File("./data/idx");
        if (!dir.exists()) dir.mkdirs();
 
        idx = new RandomAccessFile("./data/idx/" + nomeBase + ".idx", "rw");
 
        if (idx.length() < TAM_CABECALHO) {
            idx.seek(0);
            idx.writeLong(NULO);
            idx.writeLong(0L);
            idx.writeLong(0L);
            idx.writeLong(0L);
        }
    }

    // --- indexador ---

    @Override
    public void inserir(int chave, long endereco) throws Exception {
        long raiz = getRaiz();

        if (raiz == NULO) {
            No folha = new No(true);

            folha.chaves[0] = chave;
            folha.ptrs[0] = endereco;
            folha.qtdChaves = 1;

            setRaiz(alocarNo(folha));
            incrementaTotal(1);

            return;
        }

        ResultadoInsercao promovido = inserirRecursivo(raiz, chave, endereco);

        if (promovido != null) {
            No novaRaiz = new No(false);

            novaRaiz.chaves[0] = promovido.chavePromovida;
            novaRaiz.ptrs[0] = raiz;
            novaRaiz.ptrs[1] = promovido.novoFilho;
            novaRaiz.qtdChaves = 1;

            setRaiz(alocarNo(novaRaiz));
        }

        incrementaTotal(1);
    }

    @Override
    public long buscar(int chave) throws Exception {
        long raiz = getRaiz();

        if (raiz == NULO) return NULO;

        return buscarRecursivo(raiz, chave);
    }

    @Override
    public boolean remover(int chave) throws Exception {
        long raiz = getRaiz();

        if (raiz == NULO) return false;

        boolean ok = removerNaFolha(raiz, chave);

        if (ok) incrementaTotal(-1);

        return ok;
    }

    @Override
    public boolean atualizar(int chave, long novoEndereco) throws Exception {
        long raiz = getRaiz();

        if (raiz == NULO) return false;

        return atualizarNaFolha(raiz, chave, novoEndereco);
    }

    @Override
    public long[][] listarOrdenado() throws Exception {
        long raiz = getRaiz();

        if (raiz == NULO) return new long[0][0];

        long ptr = raiz;

        No no  = lerNo(ptr);

        while (!no.folha) {
            ptr = no.ptrs[0];
            no  = lerNo(ptr);
        }

        java.util.List<long[]> lista = new java.util.ArrayList<>();

        while (ptr != NULO) {
            no = lerNo(ptr);

            for (int i = 0; i < no.qtdChaves; i++)
                if (no.ptrs[i] != NULO)
                    lista.add(new long[]{ no.chaves[i], no.ptrs[i] });

            ptr = no.ptrProximo;
        }

        return lista.toArray(new long[0][0]);
    }

    @Override
    public long getTotalChaves() throws Exception {
        idx.seek(OFFSET_TOTAL);
        return idx.readLong();
    }

    @Override
    public void close() throws Exception {
        idx.close();
    }

    // --- inserção ---

    private ResultadoInsercao inserirRecursivo(long posNo, int chave, long endereco) throws Exception {
        No no = lerNo(posNo);

        if (no.folha)
            return inserirNaFolha(posNo, no, chave, endereco);

        int i = no.qtdChaves - 1;
        while (i >= 0 && chave < no.chaves[i]) i--;

        ResultadoInsercao promovido = inserirRecursivo(no.ptrs[i + 1], chave, endereco);

        if (promovido == null) return null;

        return inserirEmInterno(posNo, no, promovido.chavePromovida, promovido.novoFilho);
    }

    private ResultadoInsercao inserirNaFolha(long posNo, No no, int chave, long endereco) throws Exception {
        if (no.qtdChaves < ORDEM) {
            int i = no.qtdChaves - 1;

            while (i >= 0 && chave < no.chaves[i]) {
                no.chaves[i + 1] = no.chaves[i];
                no.ptrs[i + 1]   = no.ptrs[i];
                i--;
            }

            no.chaves[i + 1] = chave;
            no.ptrs[i + 1]   = endereco;
            no.qtdChaves++;

            gravarNo(posNo, no);

            return null;
        }

        int[] tmpChaves = new int[ORDEM + 1];
        long[] tmpPtrs= new long[ORDEM + 1];

        int i = ORDEM - 1;
        boolean inserido = false;

        for (int j = ORDEM; j >= 0; j--) {
            if (!inserido && i >= 0 && chave < no.chaves[i]) {
                tmpChaves[j] = no.chaves[i];
                tmpPtrs[j]   = no.ptrs[i--];
            } else if (!inserido && (i < 0 || chave >= no.chaves[i])) {
                tmpChaves[j] = chave;
                tmpPtrs[j] = endereco;
                inserido = true;
            } else {
                tmpChaves[j] = no.chaves[i];
                tmpPtrs[j] = no.ptrs[i--];
            }
        }

        int meio = (ORDEM + 1) / 2;

        no.qtdChaves = meio;

        for (int k = 0; k < meio; k++) {
            no.chaves[k] = tmpChaves[k];
            no.ptrs[k] = tmpPtrs[k];
        }

        No direita = new No(true);
        direita.qtdChaves = (ORDEM + 1) - meio;

        for (int k = 0; k < direita.qtdChaves; k++) {
            direita.chaves[k] = tmpChaves[meio + k];
            direita.ptrs[k] = tmpPtrs[meio + k];
        }

        direita.ptrProximo = no.ptrProximo;
        long posDireita = alocarNo(direita);

        no.ptrProximo = posDireita;
        gravarNo(posNo, no);

        ResultadoInsercao res = new ResultadoInsercao();

        res.chavePromovida = direita.chaves[0];
        res.novoFilho = posDireita;

        return res;
    }

    private ResultadoInsercao inserirEmInterno(long posNo, No no, int chave, long filhoDir) throws Exception {
        if (no.qtdChaves < ORDEM) {
            int i = no.qtdChaves - 1;

            while (i >= 0 && chave < no.chaves[i]) {
                no.chaves[i + 1] = no.chaves[i];
                no.ptrs[i + 2]   = no.ptrs[i + 1];
                i--;
            }

            no.chaves[i + 1] = chave;
            no.ptrs[i + 2]   = filhoDir;
            no.qtdChaves++;

            gravarNo(posNo, no);

            return null;
        }

        int[] tmpChaves = new int[ORDEM + 1];
        long[] tmpPtrs = new long[ORDEM + 2];

        for (int k = 0; k < ORDEM;  k++) tmpChaves[k] = no.chaves[k];
        for (int k = 0; k <= ORDEM; k++) tmpPtrs[k]   = no.ptrs[k];

        int i = ORDEM - 1;

        while (i >= 0 && chave < tmpChaves[i]) {
            tmpChaves[i + 1] = tmpChaves[i];
            tmpPtrs[i + 2]   = tmpPtrs[i + 1];
            i--;
        }

        tmpChaves[i + 1] = chave;
        tmpPtrs[i + 2] = filhoDir;

        int meio = ORDEM / 2;
        int chavePromovida = tmpChaves[meio];

        no.qtdChaves = meio;

        for (int k = 0; k < meio;  k++) no.chaves[k] = tmpChaves[k];
        for (int k = 0; k <= meio; k++) no.ptrs[k]   = tmpPtrs[k];

        gravarNo(posNo, no);

        No direita = new No(false);
        direita.qtdChaves = ORDEM - meio;

        for (int k = 0; k < direita.qtdChaves; k++) {
            direita.chaves[k] = tmpChaves[meio + 1 + k];
            direita.ptrs[k] = tmpPtrs[meio + 1 + k];
        }

        direita.ptrs[direita.qtdChaves] = tmpPtrs[ORDEM + 1];
        long posDireita = alocarNo(direita);

        ResultadoInsercao res = new ResultadoInsercao();
        res.chavePromovida = chavePromovida;
        res.novoFilho = posDireita;

        return res;
    }

    // --- busca ---

    private long buscarRecursivo(long posNo, int chave) throws Exception {
        No no = lerNo(posNo);

        if (no.folha) {
            for (int i = 0; i < no.qtdChaves; i++)
                if (no.chaves[i] == chave) return no.ptrs[i];

            return NULO;
        }

        int i = no.qtdChaves - 1;
        while (i >= 0 && chave < no.chaves[i]) i--;

        return buscarRecursivo(no.ptrs[i + 1], chave);
    }

    // --- remoção ---

    private boolean removerNaFolha(long posNo, int chave) throws Exception {
        No no = lerNo(posNo);

        if (no.folha) {
            for (int i = 0; i < no.qtdChaves; i++) {
                if (no.chaves[i] == chave) {
                    for (int j = i; j < no.qtdChaves - 1; j++) {
                        no.chaves[j] = no.chaves[j + 1];
                        no.ptrs[j] = no.ptrs[j + 1];
                    }

                    no.qtdChaves--;
                    gravarNo(posNo, no);

                    return true;
                }
            }

            return false;
        }

        int i = no.qtdChaves - 1;
        while (i >= 0 && chave < no.chaves[i]) i--;

        return removerNaFolha(no.ptrs[i + 1], chave);
    }

    // --- atualização ---

    private boolean atualizarNaFolha(long posNo, int chave, long novoEndereco) throws Exception {
        No no = lerNo(posNo);

        if (no.folha) {
            for (int i = 0; i < no.qtdChaves; i++) {
                if (no.chaves[i] == chave) {
                    no.ptrs[i] = novoEndereco;
                    gravarNo(posNo, no);

                    return true;
                }
            }

            return false;
        }

        int i = no.qtdChaves - 1;
        while (i >= 0 && chave < no.chaves[i]) i--;

        return atualizarNaFolha(no.ptrs[i + 1], chave, novoEndereco);
    }

    // --- I/O de nós ---

    private No lerNo(long posicao) throws Exception {
        idx.seek(posicao);
        No no = new No(idx.readBoolean());
        no.qtdChaves = idx.readInt();

        for (int i = 0; i < ORDEM; i++) {
            no.chaves[i] = idx.readInt();
            no.ptrs[i] = idx.readLong();
        }

        if (no.folha) no.ptrProximo = idx.readLong();
        else no.ptrs[ORDEM] = idx.readLong();

        return no;
    }

    private void gravarNo(long posicao, No no) throws Exception {
        idx.seek(posicao);
        idx.writeBoolean(no.folha);
        idx.writeInt(no.qtdChaves);

        for (int i = 0; i < ORDEM; i++) {
            idx.writeInt(no.chaves[i]);
            idx.writeLong(no.ptrs[i]);
        }

        if (no.folha) idx.writeLong(no.ptrProximo);
        else idx.writeLong(no.ptrs[ORDEM]);
    }

    private long alocarNo(No no) throws Exception {
        long posicao = Math.max(idx.length(), TAM_CABECALHO);

        gravarNo(posicao, no);

        return posicao;
    }

    // --- cabeçalho ---

    private long getRaiz() throws Exception {
        idx.seek(OFFSET_RAIZ);
        return idx.readLong();
    }

    private void setRaiz(long posicao) throws Exception {
        idx.seek(OFFSET_RAIZ);
        idx.writeLong(posicao);
    }

    private void incrementaTotal(long delta) throws Exception {
        idx.seek(OFFSET_TOTAL);
        idx.writeLong(idx.readLong() + delta);
    }

    // --- classes internas ---

    private static class No {
        boolean folha;
        int qtdChaves;
        int[] chaves = new int[ORDEM + 1];
        long[] ptrs = new long[ORDEM + 2];
        long ptrProximo = NULO;

        No(boolean folha) {
            this.folha = folha;
            java.util.Arrays.fill(ptrs, NULO);
        }
    }

    private static class ResultadoInsercao {
        int  chavePromovida;
        long novoFilho;
    }
}