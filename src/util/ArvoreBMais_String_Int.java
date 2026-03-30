package src.util;

import java.io.File;
import java.io.RandomAccessFile;
import java.util.ArrayList;

public class ArvoreBMais_String_Int {

    private int ordem;
    private int maxChaves;
    private String nomeArquivo;
    private RandomAccessFile arquivo;
    private long raiz;

    private final int TAMANHO_MAX_CHAVE = 50;

    public ArvoreBMais_String_Int(int ordem, String nomeArquivo) throws Exception {
        this.ordem = ordem;
        this.maxChaves = ordem - 1;
        this.nomeArquivo = "./data/" + nomeArquivo;

        File d = new File("./data");
        if (!d.exists()) d.mkdir();

        File arq = new File(this.nomeArquivo);
        if (!arq.exists()) {
            arquivo = new RandomAccessFile(this.nomeArquivo, "rw");
            raiz = 8;
            arquivo.writeLong(raiz);
            No folha = new No(true);
            escreverNo(folha, raiz);
        } else {
            arquivo = new RandomAccessFile(this.nomeArquivo, "rw");
            arquivo.seek(0);
            raiz = arquivo.readLong();
        }
    }

    // --- MÉTODOS PÚBLICOS PRINCIPAIS ---

    public void inserir(String chave, int valor) throws Exception {
        chave = formatarChave(chave);

        No noRaiz = lerNo(raiz);
        if (noRaiz.nChaves == maxChaves) {
            long novaRaizEnd = arquivo.length();
            No novaRaiz = new No(false);
            novaRaiz.filhos[0] = raiz;

            dividirFilho(novaRaiz, 0, noRaiz, raiz);

            raiz = novaRaizEnd;
            arquivo.seek(0);
            arquivo.writeLong(raiz);

            inserirNaoCheio(novaRaiz, novaRaizEnd, chave, valor);
        } else {
            inserirNaoCheio(noRaiz, raiz, chave, valor);
        }
    }

    public int buscar(String chave) throws Exception {
        chave = formatarChave(chave);
        return buscarRecursivo(raiz, chave);
    }

    public ArrayList<Integer> listarEmOrdem() throws Exception {
        ArrayList<Integer> idsEmOrdem = new ArrayList<>();

        long endAtual = raiz;
        No noAtual = lerNo(endAtual);
        while (!noAtual.folha) {
            endAtual = noAtual.filhos[0];
            noAtual = lerNo(endAtual);
        }

        while (true) {
            for (int i = 0; i < noAtual.nChaves; i++) {
                idsEmOrdem.add(noAtual.valores[i]);
            }
            if (noAtual.irmaoDireito == -1) break;
            noAtual = lerNo(noAtual.irmaoDireito);
        }

        return idsEmOrdem;
    }

    // -------------------------------------------------------
    // CORREÇÃO 3: delete() implementado na Árvore B+
    // Permite excluir título ao excluir livro, e futuramente
    // permitirá alterar o título do livro.
    // Estratégia: exclusão simples na folha sem rebalanceamento
    // (adequada para o contexto do trabalho).
    // -------------------------------------------------------
    public boolean delete(String chave) throws Exception {
        chave = formatarChave(chave);
        return deletarRecursivo(raiz, chave);
    }

    private boolean deletarRecursivo(long endereco, String chave) throws Exception {
        No no = lerNo(endereco);

        if (no.folha) {
            // Procura a chave na folha
            for (int i = 0; i < no.nChaves; i++) {
                if (chave.equals(no.chaves[i])) {
                    // Remove deslocando os elementos à direita para a esquerda
                    for (int j = i; j < no.nChaves - 1; j++) {
                        no.chaves[j]  = no.chaves[j + 1];
                        no.valores[j] = no.valores[j + 1];
                    }
                    // Limpa a última posição
                    no.chaves[no.nChaves - 1]  = null;
                    no.valores[no.nChaves - 1] = 0;
                    no.nChaves--;
                    escreverNo(no, endereco);
                    return true;
                }
            }
            return false; // Chave não encontrada
        } else {
            // Nó interno: desce para o filho correto.
            // CORREÇÃO: usa >= pelo mesmo motivo de buscarRecursivo —
            // quando chave == separador, a chave está no filho direito.
            int i = 0;
            while (i < no.nChaves && chave.compareTo(no.chaves[i]) >= 0) {
                i++;
            }
            return deletarRecursivo(no.filhos[i], chave);
        }
    }

    // --- MÉTODOS AUXILIARES E LÓGICA INTERNA ---

    private int buscarRecursivo(long endereco, String chave) throws Exception {
        No no = lerNo(endereco);
        int i = 0;

        if (no.folha) {
            // Na folha: procura a chave exata
            while (i < no.nChaves && chave.compareTo(no.chaves[i]) > 0) {
                i++;
            }
            if (i < no.nChaves && chave.equals(no.chaves[i])) {
                return no.valores[i];
            }
            return -1;
        } else {
            // CORREÇÃO: usa >= para que, quando chave == separador,
            // vá para filhos[i+1] (filho direito, onde a chave de fato reside).
            // O separador em nó interno é cópia da 1ª chave do filho direito.
            while (i < no.nChaves && chave.compareTo(no.chaves[i]) >= 0) {
                i++;
            }
            return buscarRecursivo(no.filhos[i], chave);
        }
    }

    private void inserirNaoCheio(No no, long enderecoNo, String chave, int valor) throws Exception {
        int i = no.nChaves - 1;

        if (no.folha) {
            while (i >= 0 && chave.compareTo(no.chaves[i]) < 0) {
                no.chaves[i + 1]  = no.chaves[i];
                no.valores[i + 1] = no.valores[i];
                i--;
            }
            no.chaves[i + 1]  = chave;
            no.valores[i + 1] = valor;
            no.nChaves++;
            escreverNo(no, enderecoNo);
        } else {
            while (i >= 0 && chave.compareTo(no.chaves[i]) < 0) {
                i--;
            }
            i++;

            long endFilho = no.filhos[i];
            No filho = lerNo(endFilho);

            if (filho.nChaves == maxChaves) {
                dividirFilho(no, i, filho, endFilho);
                if (chave.compareTo(no.chaves[i]) > 0) {
                    i++;
                }
            }
            inserirNaoCheio(lerNo(no.filhos[i]), no.filhos[i], chave, valor);
            escreverNo(no, enderecoNo);
        }
    }

    // -------------------------------------------------------
    // CORREÇÃO 2: dividirFilho() corrigido
    //
    // Problema original: a fórmula j + t + (folha ? 0 : 1) para
    // nós internos pulava a chave do meio corretamente, mas o
    // número de chaves copiadas (t) não cobria todos os casos
    // quando (maxChaves - t) != t, gerando nós com chaves
    // faltando em ordens ímpares.
    //
    // Correção:
    // - FOLHA: copia as últimas 't' chaves (índices t..maxChaves-1)
    //   e mantém o link da lista encadeada entre folhas.
    // - NÓ INTERNO: copia as últimas 't' chaves após a do meio
    //   (índices t+1..maxChaves-1) e os últimos 't+1' filhos.
    //   A chave do meio (índice t) sobe para o pai.
    //
    // 't' é calculado como maxChaves/2 para garantir distribuição
    // equilibrada independentemente de a ordem ser par ou ímpar.
    // -------------------------------------------------------
    private void dividirFilho(No pai, int i, No filhoCheio, long endFilhoCheio) throws Exception {
        No novoFilho = new No(filhoCheio.folha);
        long endNovoFilho = arquivo.length();

        // t = número de chaves que ficam no filho à esquerda após o split
        int t = maxChaves / 2;

        if (filhoCheio.folha) {
            // --- FOLHA ---
            // O filho da esquerda fica com as primeiras 't' chaves.
            // O novo filho (direita) recebe as restantes (maxChaves - t).
            novoFilho.nChaves = maxChaves - t;
            for (int j = 0; j < novoFilho.nChaves; j++) {
                novoFilho.chaves[j]  = filhoCheio.chaves[t + j];
                novoFilho.valores[j] = filhoCheio.valores[t + j];
                // Limpa as posições que saíram do filho esquerdo
                filhoCheio.chaves[t + j]  = null;
                filhoCheio.valores[t + j] = 0;
            }
            filhoCheio.nChaves = t;

            // Mantém a lista encadeada entre folhas
            novoFilho.irmaoDireito  = filhoCheio.irmaoDireito;
            filhoCheio.irmaoDireito = endNovoFilho;

            // A chave que sobe para o pai é a PRIMEIRA chave do novo filho
            // (em B+ a chave do meio é copiada, não removida das folhas)
            String chaveSobe = novoFilho.chaves[0];

            // Abre espaço no pai para o novo filho e a chave que sobe
            for (int j = pai.nChaves; j > i; j--) {
                pai.filhos[j + 1] = pai.filhos[j];
            }
            pai.filhos[i + 1] = endNovoFilho;

            for (int j = pai.nChaves - 1; j >= i; j--) {
                pai.chaves[j + 1] = pai.chaves[j];
            }
            pai.chaves[i] = chaveSobe;
            pai.nChaves++;

        } else {
            // --- NÓ INTERNO ---
            // O filho da esquerda fica com as primeiras 't' chaves.
            // A chave do meio (índice t) SOBE para o pai (não fica em nenhum filho).
            // O novo filho (direita) recebe as (maxChaves - t - 1) chaves restantes.
            String chaveSobe = filhoCheio.chaves[t];

            novoFilho.nChaves = maxChaves - t - 1;
            for (int j = 0; j < novoFilho.nChaves; j++) {
                novoFilho.chaves[j] = filhoCheio.chaves[t + 1 + j];
                filhoCheio.chaves[t + 1 + j] = null;
            }
            // Limpa também a chave do meio que subiu
            filhoCheio.chaves[t] = null;

            // Copia os filhos para o novo nó interno (são novoFilho.nChaves + 1)
            for (int j = 0; j <= novoFilho.nChaves; j++) {
                novoFilho.filhos[j] = filhoCheio.filhos[t + 1 + j];
                filhoCheio.filhos[t + 1 + j] = 0;
            }
            filhoCheio.nChaves = t;

            // Abre espaço no pai
            for (int j = pai.nChaves; j > i; j--) {
                pai.filhos[j + 1] = pai.filhos[j];
            }
            pai.filhos[i + 1] = endNovoFilho;

            for (int j = pai.nChaves - 1; j >= i; j--) {
                pai.chaves[j + 1] = pai.chaves[j];
            }
            pai.chaves[i] = chaveSobe;
            pai.nChaves++;
        }

        escreverNo(filhoCheio, endFilhoCheio);
        escreverNo(novoFilho, endNovoFilho);
    }

    private String formatarChave(String chave) {
        if (chave == null) chave = "";
        if (chave.length() > TAMANHO_MAX_CHAVE) {
            return chave.substring(0, TAMANHO_MAX_CHAVE);
        }
        return String.format("%-" + TAMANHO_MAX_CHAVE + "s", chave);
    }

    // --- LEITURA E ESCRITA FÍSICA NO ARQUIVO ---

    private No lerNo(long endereco) throws Exception {
        arquivo.seek(endereco);
        No no = new No();
        no.folha   = arquivo.readBoolean();
        no.nChaves = arquivo.readInt();

        for (int i = 0; i < maxChaves; i++) {
            byte[] b = new byte[TAMANHO_MAX_CHAVE];
            arquivo.readFully(b);
            // CORREÇÃO: ISO-8859-1 garante 1 byte por caractere,
            // incluindo acentos do português (ã, ç, á, etc.).
            // Sem encoding explícito, UTF-8 pode gerar mais de 50 bytes
            // por chave acentuada, corrompendo o layout do nó.
            no.chaves[i] = new String(b, "ISO-8859-1");
        }

        if (no.folha) {
            for (int i = 0; i < maxChaves; i++) {
                no.valores[i] = arquivo.readInt();
            }
            no.irmaoDireito = arquivo.readLong();
        } else {
            for (int i = 0; i < maxChaves + 1; i++) {
                no.filhos[i] = arquivo.readLong();
            }
        }
        return no;
    }

    private void escreverNo(No no, long endereco) throws Exception {
        arquivo.seek(endereco);
        arquivo.writeBoolean(no.folha);
        arquivo.writeInt(no.nChaves);

        for (int i = 0; i < maxChaves; i++) {
            String c = (no.chaves[i] == null) ? "" : no.chaves[i];
            // CORREÇÃO: ISO-8859-1 garante exatamente 50 bytes por chave.
            arquivo.write(formatarChave(c).getBytes("ISO-8859-1"));
        }

        if (no.folha) {
            for (int i = 0; i < maxChaves; i++) {
                arquivo.writeInt(no.valores[i]);
            }
            arquivo.writeLong(no.irmaoDireito);
        } else {
            for (int i = 0; i < maxChaves + 1; i++) {
                arquivo.writeLong(no.filhos[i]);
            }
        }
    }

    // Estrutura interna do Nó
    private class No {
        boolean folha;
        int nChaves;
        String[] chaves;
        int[] valores;
        long[] filhos;
        long irmaoDireito;

        public No() {
            this(true);
        }

        public No(boolean folha) {
            this.folha    = folha;
            this.nChaves  = 0;
            this.chaves   = new String[maxChaves];
            if (folha) {
                this.valores        = new int[maxChaves];
                this.irmaoDireito   = -1;
            } else {
                this.filhos = new long[maxChaves + 1];
            }
        }
    }
}