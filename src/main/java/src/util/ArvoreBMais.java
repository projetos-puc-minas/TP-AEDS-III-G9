package src.util;

import java.io.File;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ArvoreBMais implements Indexador {

    public static final int ORDEM = 4;
    private static final int TAM_CABECALHO = 32;
    public static final long NULO = -1L;

    private static final int OFFSET_RAIZ = 0;
    private static final int OFFSET_TOTAL = 8;

    public static final int TAM_NO =
            1 + 4 + ORDEM * (4 + 8) + 8;

    private final RandomAccessFile idx;

    private final int minKeys = (ORDEM + 1) / 2;

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
        return buscarNaFolha(raiz, chave);
    }

    public boolean remover(int chave) throws Exception {
        long raiz = getRaiz();
        if (raiz == NULO) return false;
        boolean ok = removerRecursivo(raiz, chave, NULO, -1);
        if (ok) incrementaTotal(-1);
        return ok;
    }

    @Override
    public boolean atualizar(int chave, long novoEndereco) throws Exception {
        long raiz = getRaiz();
        if (raiz == NULO) return false;
        return atualizarNaFolha(raiz, chave, novoEndereco);
    }

    public long[][] listarOrdenado() throws Exception {
        long raiz = getRaiz();
        if (raiz == NULO) return new long[0][0];

        long ptr = raiz;
        No no = lerNo(ptr);
        while (!no.folha) {
            ptr = no.ptrs[0];
            no = lerNo(ptr);
        }

        List<long[]> lista = new ArrayList<>();
        while (ptr != NULO) {
            no = lerNo(ptr);
            for (int i = 0; i < no.qtdChaves; i++) {
                if (no.ptrs[i] != NULO) {
                    lista.add(new long[]{no.chaves[i], no.ptrs[i]});
                }
            }
            ptr = no.ptrProximo;
        }

        return lista.toArray(new long[0][0]);
    }

    public long[][] listarOrdenadoDecrescente() throws Exception {
        long[][] crescente = listarOrdenado();

        int esq = 0, dir = crescente.length - 1;
        while (esq < dir) {
            long[] tmp = crescente[esq];
            crescente[esq] = crescente[dir];
            crescente[dir] = tmp;
            esq++;
            dir--;
        }

        return crescente;
    }

    public long getTotalChaves() throws Exception {
        idx.seek(OFFSET_TOTAL);
        return idx.readLong();
    }

    @Override
    public void close() throws Exception {
        idx.close();
    }

    private long buscarNaFolha(long posRaiz, int chave) throws Exception {
        long pos = posRaiz;
        No no = lerNo(pos);

        while (!no.folha) {
            pos = filhoParaBusca(no, chave);
            no = lerNo(pos);
        }

        int i = buscaBinaria(no.chaves, 0, no.qtdChaves - 1, chave);
        return (i >= 0) ? no.ptrs[i] : NULO;
    }

    private long filhoParaBusca(No no, int chave) {
        int lo = 0, hi = no.qtdChaves - 1;
        while (lo <= hi) {
            int mid = (lo + hi) >>> 1;
            if (chave < no.chaves[mid]) hi = mid - 1;
            else if (chave > no.chaves[mid]) lo = mid + 1;
            else return no.ptrs[mid + 1];
        }
        return no.ptrs[lo];
    }

    private int buscaBinaria(int[] arr, int l, int r, int chave) {
        while (l <= r) {
            int m = (l + r) >>> 1;
            if (chave < arr[m]) r = m - 1;
            else if (chave > arr[m]) l = m + 1;
            else return m;
        }
        return -1;
    }

    private ResultadoInsercao inserirRecursivo(long posNo, int chave, long endereco) throws Exception {
        No no = lerNo(posNo);

        if (no.folha) return inserirNaFolha(posNo, no, chave, endereco);

        long posFilho = filhoParaBusca(no, chave);
        ResultadoInsercao promovido = inserirRecursivo(posFilho, chave, endereco);

        if (promovido == null) return null;
        return inserirEmInterno(posNo, no, promovido.chavePromovida, promovido.novoFilho);
    }

    private ResultadoInsercao inserirNaFolha(long posNo, No no, int chave, long endereco) throws Exception {
        int pos = buscaBinaria(no.chaves, 0, no.qtdChaves - 1, chave);
        if (pos >= 0) {
            no.ptrs[pos] = endereco;
            gravarNo(posNo, no);
            return null;
        }

        if (no.qtdChaves < ORDEM) {
            int i = no.qtdChaves - 1;
            while (i >= 0 && chave < no.chaves[i]) {
                no.chaves[i + 1] = no.chaves[i];
                no.ptrs[i + 1] = no.ptrs[i];
                i--;
            }
            no.chaves[i + 1] = chave;
            no.ptrs[i + 1] = endereco;
            no.qtdChaves++;
            gravarNo(posNo, no);
            return null;
        }

        int[] tmpChaves = new int[ORDEM + 1];
        long[] tmpPtrs = new long[ORDEM + 1];

        boolean inserido = false;
        int src = 0;
        for (int dst = 0; dst <= ORDEM; dst++) {
            if (!inserido && (src >= ORDEM || chave <= no.chaves[src])) {
                tmpChaves[dst] = chave;
                tmpPtrs[dst] = endereco;
                inserido = true;
            } else {
                tmpChaves[dst] = no.chaves[src];
                tmpPtrs[dst] = no.ptrs[src];
                src++;
            }
        }

        int meio = ORDEM / 2;

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
                no.ptrs[i + 2] = no.ptrs[i + 1];
                i--;
            }
            no.chaves[i + 1] = chave;
            no.ptrs[i + 2] = filhoDir;
            no.qtdChaves++;
            gravarNo(posNo, no);
            return null;
        }

        int[] tmpChaves = new int[ORDEM + 1];
        long[] tmpPtrs = new long[ORDEM + 2];

        for (int k = 0; k < ORDEM; k++) tmpChaves[k] = no.chaves[k];
        for (int k = 0; k <= ORDEM; k++) tmpPtrs[k] = no.ptrs[k];

        int i = ORDEM - 1;
        while (i >= 0 && chave < tmpChaves[i]) {
            tmpChaves[i + 1] = tmpChaves[i];
            tmpPtrs[i + 2] = tmpPtrs[i + 1];
            i--;
        }
        tmpChaves[i + 1] = chave;
        tmpPtrs[i + 2] = filhoDir;

        int meio = ORDEM / 2;
        int chavePromovida = tmpChaves[meio];

        no.qtdChaves = meio;
        for (int k = 0; k < meio; k++) no.chaves[k] = tmpChaves[k];
        for (int k = 0; k <= meio; k++) no.ptrs[k] = tmpPtrs[k];
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

    private boolean removerRecursivo(long posNo, int chave, long posNoPai, int idxNoPai) throws Exception {
        No no = lerNo(posNo);

        if (no.folha) {
            int pos = buscaBinaria(no.chaves, 0, no.qtdChaves - 1, chave);
            if (pos < 0) return false;

            for (int j = pos; j < no.qtdChaves - 1; j++) {
                no.chaves[j] = no.chaves[j + 1];
                no.ptrs[j] = no.ptrs[j + 1];
            }
            no.qtdChaves--;
            gravarNo(posNo, no);

            if (posNoPai == NULO || no.qtdChaves >= minKeys) {
                if (posNoPai != NULO && pos == 0 && no.qtdChaves > 0) {
                    atualizarSeparadorNoPai(posNoPai, idxNoPai, no.chaves[0]);
                }
                return true;
            }

            tratarUnderflowFolha(posNo, no, posNoPai, idxNoPai);
            return true;
        }

        int i = no.qtdChaves - 1;
        while (i >= 0 && chave < no.chaves[i]) i--;
        int idxFilho = i + 1;
        long posFilho = no.ptrs[idxFilho];

        return removerRecursivo(posFilho, chave, posNo, idxFilho);
    }

    private void tratarUnderflowFolha(long posNo, No no, long posNoPai, int idxNoPai) throws Exception {
        No pai = lerNo(posNoPai);

        long posIrmaoEsq = (idxNoPai > 0) ? pai.ptrs[idxNoPai - 1] : NULO;
        long posIrmaoDir = (idxNoPai < pai.qtdChaves) ? pai.ptrs[idxNoPai + 1] : NULO;

        if (posIrmaoEsq != NULO) {
            No irmaoEsq = lerNo(posIrmaoEsq);
            if (irmaoEsq.qtdChaves > minKeys) {
                int chavePegada = irmaoEsq.chaves[irmaoEsq.qtdChaves - 1];
                long ptrPegado = irmaoEsq.ptrs[irmaoEsq.qtdChaves - 1];
                irmaoEsq.qtdChaves--;
                gravarNo(posIrmaoEsq, irmaoEsq);

                for (int k = no.qtdChaves; k > 0; k--) {
                    no.chaves[k] = no.chaves[k - 1];
                    no.ptrs[k] = no.ptrs[k - 1];
                }
                no.chaves[0] = chavePegada;
                no.ptrs[0] = ptrPegado;
                no.qtdChaves++;
                gravarNo(posNo, no);

                atualizarSeparadorNoPai(posNoPai, idxNoPai, no.chaves[0]);
                return;
            }
        }

        if (posIrmaoDir != NULO) {
            No irmaoDir = lerNo(posIrmaoDir);
            if (irmaoDir.qtdChaves > minKeys) {
                no.chaves[no.qtdChaves] = irmaoDir.chaves[0];
                no.ptrs[no.qtdChaves] = irmaoDir.ptrs[0];
                no.qtdChaves++;
                gravarNo(posNo, no);

                for (int k = 0; k < irmaoDir.qtdChaves - 1; k++) {
                    irmaoDir.chaves[k] = irmaoDir.chaves[k + 1];
                    irmaoDir.ptrs[k] = irmaoDir.ptrs[k + 1];
                }
                irmaoDir.qtdChaves--;
                gravarNo(posIrmaoDir, irmaoDir);

                atualizarSeparadorNoPai(posNoPai, idxNoPai + 1, irmaoDir.chaves[0]);
                return;
            }
        }

        if (posIrmaoEsq != NULO) {
            fundirFolhas(posIrmaoEsq, posNo, posNoPai, idxNoPai - 1);
        } else {
            fundirFolhas(posNo, posIrmaoDir, posNoPai, idxNoPai);
        }
    }

    private void fundirFolhas(long posEsq, long posDir, long posNoPai, int separadorIdx) throws Exception {
        No esq = lerNo(posEsq);
        No dir = lerNo(posDir);
        No pai = lerNo(posNoPai);

        for (int k = 0; k < dir.qtdChaves; k++) {
            esq.chaves[esq.qtdChaves + k] = dir.chaves[k];
            esq.ptrs[esq.qtdChaves + k] = dir.ptrs[k];
        }
        esq.qtdChaves += dir.qtdChaves;
        esq.ptrProximo = dir.ptrProximo;
        gravarNo(posEsq, esq);

        for (int k = separadorIdx; k < pai.qtdChaves - 1; k++) {
            pai.chaves[k] = pai.chaves[k + 1];
            pai.ptrs[k + 1] = pai.ptrs[k + 2];
        }
        pai.qtdChaves--;

        long posRaiz = getRaiz();
        if (posNoPai == posRaiz && pai.qtdChaves == 0) {
            setRaiz(posEsq);
        } else {
            gravarNo(posNoPai, pai);
            if (posNoPai != posRaiz && pai.qtdChaves < minKeys) {
                tratarUnderflowInterno(posNoPai, pai);
            }
        }
    }

    private void tratarUnderflowInterno(long posNo, No no) throws Exception {
        long posRaiz = getRaiz();
        long posNoPai = encontrarPai(posRaiz, posNo);
        if (posNoPai == NULO) return;

        No pai = lerNo(posNoPai);
        int idxNoPai = 0;
        
        while (idxNoPai <= pai.qtdChaves && pai.ptrs[idxNoPai] != posNo) idxNoPai++;

        long posIrmaoEsq = (idxNoPai > 0) ? pai.ptrs[idxNoPai - 1] : NULO;
        long posIrmaoDir = (idxNoPai < pai.qtdChaves) ? pai.ptrs[idxNoPai + 1] : NULO;

        if (posIrmaoEsq != NULO) {
            No irmaoEsq = lerNo(posIrmaoEsq);
            
            if (irmaoEsq.qtdChaves > minKeys) {
                int chaveDesce = pai.chaves[idxNoPai - 1];
                long posFilhoMov = irmaoEsq.ptrs[irmaoEsq.qtdChaves];

                for (int k = no.qtdChaves; k > 0; k--) {
                    no.chaves[k] = no.chaves[k - 1];
                    no.ptrs[k + 1] = no.ptrs[k];
                }
                
                no.ptrs[1] = no.ptrs[0];
                no.chaves[0] = chaveDesce;
                no.ptrs[0] = posFilhoMov;
                no.qtdChaves++;
                
                gravarNo(posNo, no);

                pai.chaves[idxNoPai - 1] = irmaoEsq.chaves[irmaoEsq.qtdChaves - 1];
                irmaoEsq.qtdChaves--;
                
                gravarNo(posIrmaoEsq, irmaoEsq);
                gravarNo(posNoPai, pai);
                
                return;
            }
        }

        if (posIrmaoDir != NULO) {
            No irmaoDir = lerNo(posIrmaoDir);
            
            if (irmaoDir.qtdChaves > minKeys) {
                no.chaves[no.qtdChaves] = pai.chaves[idxNoPai];
                no.ptrs[no.qtdChaves + 1] = irmaoDir.ptrs[0];
                no.qtdChaves++;
                
                gravarNo(posNo, no);

                pai.chaves[idxNoPai] = irmaoDir.chaves[0];
                for (int k = 0; k < irmaoDir.qtdChaves - 1; k++) {
                    irmaoDir.chaves[k] = irmaoDir.chaves[k + 1];
                    irmaoDir.ptrs[k] = irmaoDir.ptrs[k + 1];
                }
                
                irmaoDir.ptrs[irmaoDir.qtdChaves - 1] = irmaoDir.ptrs[irmaoDir.qtdChaves];
                irmaoDir.qtdChaves--;
                
                gravarNo(posIrmaoDir, irmaoDir);
                gravarNo(posNoPai, pai);
                
                return;
            }
        }

        if (posIrmaoEsq != NULO) {
            fundirInternos(posIrmaoEsq, posNo, posNoPai, idxNoPai - 1);
        } else {
            fundirInternos(posNo, posIrmaoDir, posNoPai, idxNoPai);
        }
    }

    private void fundirInternos(long posEsq, long posDir, long posNoPai, int separadorIdx) throws Exception {
        No esq = lerNo(posEsq);
        No dir = lerNo(posDir);
        No pai = lerNo(posNoPai);

        esq.chaves[esq.qtdChaves] = pai.chaves[separadorIdx];
        esq.qtdChaves++;

        for (int k = 0; k < dir.qtdChaves; k++) {
            esq.chaves[esq.qtdChaves + k] = dir.chaves[k];
            esq.ptrs[esq.qtdChaves + k] = dir.ptrs[k];
        }
        
        esq.ptrs[esq.qtdChaves + dir.qtdChaves] = dir.ptrs[dir.qtdChaves];
        esq.qtdChaves += dir.qtdChaves;
        gravarNo(posEsq, esq);

        for (int k = separadorIdx; k < pai.qtdChaves - 1; k++) {
            pai.chaves[k] = pai.chaves[k + 1];
            pai.ptrs[k + 1] = pai.ptrs[k + 2];
        }
        pai.qtdChaves--;

        long posRaiz = getRaiz();
        if (posNoPai == posRaiz && pai.qtdChaves == 0) {
            setRaiz(posEsq);
        } else {
            gravarNo(posNoPai, pai);
            
            if (posNoPai != posRaiz && pai.qtdChaves < minKeys) {
                tratarUnderflowInterno(posNoPai, pai);
            }
        }
    }

    private long encontrarPai(long posAtual, long posFilho) throws Exception {
        No no = lerNo(posAtual);
        if (no.folha) return NULO;

        for (int i = 0; i <= no.qtdChaves; i++) {
            if (no.ptrs[i] == posFilho) return posAtual;
            
            long resultado = encontrarPai(no.ptrs[i], posFilho);
            if (resultado != NULO) return resultado;
        }
        
        return NULO;
    }

    private void atualizarSeparadorNoPai(long posNoPai, int idxNoPai, int novaChave) throws Exception {
        if (posNoPai == NULO || idxNoPai <= 0) return;
        No pai = lerNo(posNoPai);
        pai.chaves[idxNoPai - 1] = novaChave;
        gravarNo(posNoPai, pai);
    }

    private boolean atualizarNaFolha(long posNo, int chave, long novoEndereco) throws Exception {
        No no = lerNo(posNo);

        if (no.folha) {
            int pos = buscaBinaria(no.chaves, 0, no.qtdChaves - 1, chave);
            if (pos < 0) return false;
            
            no.ptrs[pos] = novoEndereco;
            gravarNo(posNo, no);
            
            return true;
        }

        long posFilho = filhoParaBusca(no, chave);
        
        return atualizarNaFolha(posFilho, chave, novoEndereco);
    }

    private No lerNo(long posicao) throws Exception {
        idx.seek(posicao);
        No no = new No(idx.readBoolean());
        no.qtdChaves = idx.readInt();
        
        for (int i = 0; i < ORDEM; i++) {
            no.chaves[i] = idx.readInt();
            no.ptrs[i] = idx.readLong();
        }
        
        long ultimo = idx.readLong();
        if (no.folha) no.ptrProximo = ultimo;
        else no.ptrs[ORDEM] = ultimo;
        
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
        
        long ultimo = no.folha ? no.ptrProximo : no.ptrs[ORDEM];
        idx.writeLong(ultimo);
    }

    private long alocarNo(No no) throws Exception {
        long posicao = Math.max(idx.length(), TAM_CABECALHO);
        gravarNo(posicao, no);
        
        return posicao;
    }

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

    private static class No {
        boolean folha;
        int qtdChaves;
        int[] chaves = new int[ORDEM + 1];
        long[] ptrs = new long[ORDEM + 2];
        long ptrProximo = NULO;

        No(boolean folha) {
            this.folha = folha;
            Arrays.fill(ptrs, NULO);
        }
    }

    private static class ResultadoInsercao {
        int chavePromovida;
        long novoFilho;
    }
}
