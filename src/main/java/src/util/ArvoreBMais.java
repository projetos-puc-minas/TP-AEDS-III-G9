package src.util;

import java.io.File;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ArvoreBMais implements Indexador {

    public  static final int  ORDEM         = 4;
    private static final int  TAM_CABECALHO = 32;
    public  static final long NULO          = -1L;

    private static final int OFFSET_RAIZ  = 0;
    private static final int OFFSET_TOTAL = 8;

    public static final int TAM_NO =
            1 + 4 + ORDEM * (4 + 8) + 8;

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

    // Indexador

    @Override
    public void inserir(int chave, long endereco) throws Exception {
        long raiz = getRaiz();

        if (raiz == NULO) {
            No folha = new No(true);
            folha.chaves[0] = chave;
            folha.ptrs[0]   = endereco;
            folha.qtdChaves = 1;
            setRaiz(alocarNo(folha));
            incrementaTotal(1);
            return;
        }

        ResultadoInsercao promovido = inserirRecursivo(raiz, chave, endereco);

        if (promovido != null) {
            No novaRaiz = new No(false);
            novaRaiz.chaves[0] = promovido.chavePromovida;
            novaRaiz.ptrs[0]   = raiz;
            novaRaiz.ptrs[1]   = promovido.novoFilho;
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

    public long[][] listarOrdenado() throws Exception {
        long raiz = getRaiz();
        if (raiz == NULO) return new long[0][0];

        // Desce até a folha mais à esquerda
        long ptr = raiz;
        No   no  = lerNo(ptr);
        while (!no.folha) {
            ptr = no.ptrs[0];
            no  = lerNo(ptr);
        }

        List<long[]> lista = new ArrayList<>();
        while (ptr != NULO) {
            no = lerNo(ptr);
            for (int i = 0; i < no.qtdChaves; i++) {
                if (no.ptrs[i] != NULO) {
                    lista.add(new long[]{ no.chaves[i], no.ptrs[i] });
                }
            }
            ptr = no.ptrProximo;
        }

        return lista.toArray(new long[0][0]);
    }

    // @Override
    public long[][] listarOrdenadoDecrescente() throws Exception {
        long[][] crescente = listarOrdenado();

        // Inverte o array in-place sem alocar uma nova estrutura de dados
        int esq = 0;
        int dir = crescente.length - 1;
        while (esq < dir) {
            long[] tmp       = crescente[esq];
            crescente[esq]   = crescente[dir];
            crescente[dir]   = tmp;
            esq++;
            dir--;
        }

        return crescente;
    }

    // @Override
    public long getTotalChaves() throws Exception {
        idx.seek(OFFSET_TOTAL);
        return idx.readLong();
    }

    @Override
    public void close() throws Exception {
        idx.close();
    }

    // Inserção recursiva

    private ResultadoInsercao inserirRecursivo(long posNo, int chave, long endereco) throws Exception {
        No no = lerNo(posNo);

        if (no.folha) return inserirNaFolha(posNo, no, chave, endereco);

        // Encontra o filho correto
        int i = no.qtdChaves - 1;
        while (i >= 0 && chave < no.chaves[i]) i--;
        ResultadoInsercao promovido = inserirRecursivo(no.ptrs[i + 1], chave, endereco);

        if (promovido == null) return null;
        return inserirEmInterno(posNo, no, promovido.chavePromovida, promovido.novoFilho);
    }

    private ResultadoInsercao inserirNaFolha(long posNo, No no, int chave, long endereco) throws Exception {
        if (no.qtdChaves < ORDEM) {
            // Espaço disponível — insere mantendo ordem
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

        // Folha cheia — divide
        int[]  tmpChaves = new int[ORDEM + 1];
        long[] tmpPtrs   = new long[ORDEM + 1];

        // Copia chaves existentes para tmp e insere a nova em ordem
        boolean inserido = false;
        int src = 0;
        for (int dst = 0; dst <= ORDEM; dst++) {
            if (!inserido && (src >= ORDEM || chave <= no.chaves[src])) {
                tmpChaves[dst] = chave;
                tmpPtrs[dst]   = endereco;
                inserido = true;
            } else {
                tmpChaves[dst] = no.chaves[src];
                tmpPtrs[dst]   = no.ptrs[src];
                src++;
            }
        }

        int meio = (ORDEM + 1) / 2;

        // Nó esquerdo (mantém posição original)
        no.qtdChaves = meio;
        for (int k = 0; k < meio; k++) {
            no.chaves[k] = tmpChaves[k];
            no.ptrs[k]   = tmpPtrs[k];
        }

        // Nó direito (novo)
        No direita = new No(true);
        direita.qtdChaves = (ORDEM + 1) - meio;
        for (int k = 0; k < direita.qtdChaves; k++) {
            direita.chaves[k] = tmpChaves[meio + k];
            direita.ptrs[k]   = tmpPtrs[meio + k];
        }

        direita.ptrProximo = no.ptrProximo;
        long posDireita    = alocarNo(direita);
        no.ptrProximo      = posDireita;
        gravarNo(posNo, no);

        ResultadoInsercao res = new ResultadoInsercao();
        res.chavePromovida = direita.chaves[0]; // cópia (folha mantém a chave)
        res.novoFilho      = posDireita;
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

        // Nó interno cheio — divide
        int[]  tmpChaves = new int[ORDEM + 1];
        long[] tmpPtrs   = new long[ORDEM + 2];

        for (int k = 0; k < ORDEM;     k++) tmpChaves[k] = no.chaves[k];
        for (int k = 0; k <= ORDEM;    k++) tmpPtrs[k]   = no.ptrs[k];

        int i = ORDEM - 1;
        while (i >= 0 && chave < tmpChaves[i]) {
            tmpChaves[i + 1] = tmpChaves[i];
            tmpPtrs[i + 2]   = tmpPtrs[i + 1];
            i--;
        }
        tmpChaves[i + 1] = chave;
        tmpPtrs[i + 2]   = filhoDir;

        int meio           = ORDEM / 2;
        int chavePromovida = tmpChaves[meio]; // promovida para cima (removida do nó)

        no.qtdChaves = meio;
        for (int k = 0; k < meio;  k++) no.chaves[k] = tmpChaves[k];
        for (int k = 0; k <= meio; k++) no.ptrs[k]   = tmpPtrs[k];
        gravarNo(posNo, no);

        No direita = new No(false);
        direita.qtdChaves = ORDEM - meio;
        for (int k = 0; k < direita.qtdChaves; k++) {
            direita.chaves[k] = tmpChaves[meio + 1 + k];
            direita.ptrs[k]   = tmpPtrs[meio + 1 + k];
        }
        direita.ptrs[direita.qtdChaves] = tmpPtrs[ORDEM + 1];
        long posDireita = alocarNo(direita);

        ResultadoInsercao res = new ResultadoInsercao();
        res.chavePromovida = chavePromovida;
        res.novoFilho      = posDireita;
        return res;
    }

    // Busca

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

    // Remoção (sem rebalanceamento)

    private boolean removerNaFolha(long posNo, int chave) throws Exception {
        No no = lerNo(posNo);

        if (no.folha) {
            for (int i = 0; i < no.qtdChaves; i++) {
                if (no.chaves[i] == chave) {
                    for (int j = i; j < no.qtdChaves - 1; j++) {
                        no.chaves[j] = no.chaves[j + 1];
                        no.ptrs[j]   = no.ptrs[j + 1];
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

    // Atualização

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

    // I/O de nós — layout fixo (TAM_NO bytes por nó)

    private No lerNo(long posicao) throws Exception {
        idx.seek(posicao);
        No no = new No(idx.readBoolean());   // 1 byte
        no.qtdChaves = idx.readInt();         // 4 bytes
        for (int i = 0; i < ORDEM; i++) {
            no.chaves[i] = idx.readInt();     // 4 bytes
            no.ptrs[i]   = idx.readLong();    // 8 bytes
        }
        // último ponteiro (ptrs[ORDEM] em internos, ptrProximo em folhas)
        long ultimo = idx.readLong();         // 8 bytes
        if (no.folha) no.ptrProximo  = ultimo;
        else          no.ptrs[ORDEM] = ultimo;
        return no;
    }

    private void gravarNo(long posicao, No no) throws Exception {
        idx.seek(posicao);
        idx.writeBoolean(no.folha);          // 1
        idx.writeInt(no.qtdChaves);          // 4
        for (int i = 0; i < ORDEM; i++) {
            idx.writeInt(no.chaves[i]);      // 4
            idx.writeLong(no.ptrs[i]);       // 8
        }
        long ultimo = no.folha ? no.ptrProximo : no.ptrs[ORDEM];
        idx.writeLong(ultimo);               // 8
    }

    private long alocarNo(No no) throws Exception {
        long posicao = Math.max(idx.length(), TAM_CABECALHO);
        gravarNo(posicao, no);
        return posicao;
    }

    // Cabeçalho

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

    // Classes internas

    private static class No {
        boolean folha;
        int     qtdChaves;
        int[]   chaves      = new int[ORDEM + 1];
        long[]  ptrs        = new long[ORDEM + 2];
        long    ptrProximo  = NULO;

        No(boolean folha) {
            this.folha = folha;
            Arrays.fill(ptrs, NULO);
        }
    }

    private static class ResultadoInsercao {
        int  chavePromovida;
        long novoFilho;
    }
}