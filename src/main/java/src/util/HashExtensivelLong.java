package src.util;

import java.io.File;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;

public class HashExtensivelLong implements AutoCloseable {

    private static final int  TAM_BUCKET  = 10;
    private static final int  TAM_ENTRADA = Long.BYTES + Long.BYTES; // 8 + 8 = 16
    private static final long NULO        = -1L;

    private final RandomAccessFile dir;
    private final RandomAccessFile bucket;
    private int profGlobal;

    public HashExtensivelLong(String nomeBase) throws Exception {
        new File("./data/idx").mkdirs();

        this.dir    = new RandomAccessFile("./data/idx/" + nomeBase + ".dir.bin", "rw");
        this.bucket = new RandomAccessFile("./data/idx/" + nomeBase + ".bkt.bin", "rw");

        if (this.dir.length() == 0) {
            this.profGlobal = 1;
            this.dir.writeInt(this.profGlobal);
            long end0 = criarBucket(1);
            long end1 = criarBucket(1);
            this.dir.writeLong(end0);
            this.dir.writeLong(end1);
        } else {
            this.dir.seek(0);
            this.profGlobal = this.dir.readInt();
        }
    }

    public void inserir(long chave, long enderecoNoArquivo) throws Exception {
        inserirNoBucket(chave, enderecoNoArquivo);
    }

    public long buscar(long chave) throws Exception {
        long endBkt = getEnderecoDoBucket(hash(chave));
        return buscarNoBucket(endBkt, chave);
    }

    public boolean remover(long chave) throws Exception {
        long endBkt = getEnderecoDoBucket(hash(chave));
        return removerDoBucket(endBkt, chave);
    }

    public boolean atualizar(long chave, long novoEndereco) throws Exception {
        long endBkt = getEnderecoDoBucket(hash(chave));
        return atualizarNoBucket(endBkt, chave, novoEndereco);
    }

    public long[][] listarPorFaixa(long chaveMin, long chaveMax) throws Exception {
        List<long[]> resultado = new ArrayList<>();
        int capacidade = 1 << profGlobal;
        long ultimoBkt = -1;

        for (int i = 0; i < capacidade; i++) {
            long endBkt = getEnderecoDoBucket(i);
            if (endBkt == ultimoBkt) continue;
            ultimoBkt = endBkt;

            int count = getCount(endBkt);
            long posBase = endBkt + (2L * Integer.BYTES);

            for (int j = 0; j < count; j++) {
                this.bucket.seek(posBase + (long) j * TAM_ENTRADA);
                long chave = this.bucket.readLong();
                long endereco = this.bucket.readLong();
                if (chave >= chaveMin && chave < chaveMax) {
                    resultado.add(new long[]{chave, endereco});
                }
            }
        }
        return resultado.toArray(new long[0][]);
    }

    @Override
    public void close() throws Exception {
        this.dir.close();
        this.bucket.close();
    }

    private int hash(long chave) {
        return (int) (chave & ((1 << this.profGlobal) - 1));
    }

    private long getEnderecoDoBucket(int index) throws Exception {
        this.dir.seek(Integer.BYTES + (long) index * Long.BYTES);
        return this.dir.readLong();
    }

    private void setEnderecoDoBucket(int idx, long endereco) throws Exception {
        this.dir.seek(Integer.BYTES + (long) idx * Long.BYTES);
        this.dir.writeLong(endereco);
    }

    private boolean inserirNoBucket(long chave, long valor) throws Exception {
        int idxDir = hash(chave);
        long endBkt = getEnderecoDoBucket(idxDir);

        if (atualizarNoBucket(endBkt, chave, valor)) return true;

        int count = getCount(endBkt);
        if (count < TAM_BUCKET) {
            adicionarEntrada(endBkt, chave, valor);
            return true;
        }

        int profLocal = getProfLocal(endBkt);
        if (profLocal == this.profGlobal) dobrarDiretorio();
        splitBucket(endBkt, profLocal);
        return inserirNoBucket(chave, valor);
    }

    private void dobrarDiretorio() throws Exception {
        int cap = 1 << this.profGlobal;
        long[] ptrs = new long[cap];
        for (int i = 0; i < cap; i++) ptrs[i] = getEnderecoDoBucket(i);

        this.profGlobal++;
        int novaCap = 1 << this.profGlobal;

        this.dir.seek(0);
        this.dir.writeInt(this.profGlobal);
        for (int i = 0; i < novaCap; i++) this.dir.writeLong(ptrs[i % cap]);
    }

    private void splitBucket(long endBktAntigo, int profLocal) throws Exception {
        int novaProfLocal = profLocal + 1;
        long[] chaves = new long[TAM_BUCKET];
        long[] enderecos = new long[TAM_BUCKET];
        lerEntradas(endBktAntigo, chaves, enderecos);

        long endBktNovo = criarBucket(novaProfLocal);

        this.bucket.seek(endBktAntigo);
        this.bucket.writeInt(novaProfLocal);
        this.bucket.writeInt(0);
        for (int i = 0; i < TAM_BUCKET; i++) {
            this.bucket.writeLong(0);
            this.bucket.writeLong(NULO);
        }

        int tamanho = 1 << this.profGlobal;
        for (int i = 0; i < tamanho; i++) {
            if (getEnderecoDoBucket(i) == endBktAntigo) {
                if (((i >> profLocal) & 1) == 1) setEnderecoDoBucket(i, endBktNovo);
            }
        }

        for (int i = 0; i < TAM_BUCKET; i++) {
            if (enderecos[i] != NULO) inserirNoBucket(chaves[i], enderecos[i]);
        }
    }

    private long criarBucket(int profLocal) throws Exception {
        long pos = this.bucket.length();
        this.bucket.seek(pos);
        this.bucket.writeInt(profLocal);
        this.bucket.writeInt(0);
        for (int i = 0; i < TAM_BUCKET; i++) {
            this.bucket.writeLong(0);
            this.bucket.writeLong(NULO);
        }
        return pos;
    }

    private void adicionarEntrada(long endBkt, long chave, long valor) throws Exception {
        int count = getCount(endBkt);
        long pos = endBkt + (2L * Integer.BYTES) + (long) count * TAM_ENTRADA;
        this.bucket.seek(pos);
        this.bucket.writeLong(chave);
        this.bucket.writeLong(valor);
        setCount(endBkt, count + 1);
    }

    private long buscarNoBucket(long endBkt, long chave) throws Exception {
        int count = getCount(endBkt);
        long posBase = endBkt + (2L * Integer.BYTES);
        for (int i = 0; i < count; i++) {
            this.bucket.seek(posBase + (long) i * TAM_ENTRADA);
            if (this.bucket.readLong() == chave) return this.bucket.readLong();
        }
        return NULO;
    }

    private boolean removerDoBucket(long endBkt, long chave) throws Exception {
        int count = getCount(endBkt);
        long posBase = endBkt + (2L * Integer.BYTES);
        for (int i = 0; i < count; i++) {
            long pos = posBase + (long) i * TAM_ENTRADA;
            this.bucket.seek(pos);
            if (this.bucket.readLong() == chave) {
                if (i < count - 1) {
                    long ulPos = posBase + (long)(count - 1) * TAM_ENTRADA;
                    this.bucket.seek(ulPos);
                    long kU = this.bucket.readLong();
                    long vU = this.bucket.readLong();
                    this.bucket.seek(pos);
                    this.bucket.writeLong(kU);
                    this.bucket.writeLong(vU);
                }
                setCount(endBkt, count - 1);
                return true;
            }
        }
        return false;
    }

    private boolean atualizarNoBucket(long endBkt, long chave, long novoValor) throws Exception {
        int count = getCount(endBkt);
        long posBase = endBkt + (2L * Integer.BYTES);
        for (int i = 0; i < count; i++) {
            long pos = posBase + (long) i * TAM_ENTRADA;
            this.bucket.seek(pos);
            if (this.bucket.readLong() == chave) {
                this.bucket.writeLong(novoValor);
                return true;
            }
        }
        return false;
    }

    private void lerEntradas(long endBkt, long[] chaves, long[] enderecos) throws Exception {
        int count = getCount(endBkt);
        long posBase = endBkt + (2L * Integer.BYTES);
        for (int i = 0; i < TAM_BUCKET; i++) { chaves[i] = 0; enderecos[i] = NULO; }
        for (int i = 0; i < count; i++) {
            this.bucket.seek(posBase + (long) i * TAM_ENTRADA);
            chaves[i] = this.bucket.readLong();
            enderecos[i] = this.bucket.readLong();
        }
    }

    private int getProfLocal(long endBkt) throws Exception {
        this.bucket.seek(endBkt);
        return this.bucket.readInt();
    }

    private int getCount(long endBkt) throws Exception {
        this.bucket.seek(endBkt + Integer.BYTES);
        return this.bucket.readInt();
    }

    private void setCount(long endBkt, int count) throws Exception {
        this.bucket.seek(endBkt + Integer.BYTES);
        this.bucket.writeInt(count);
    }
}
