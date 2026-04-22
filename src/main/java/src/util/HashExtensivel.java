package src.util;

import java.io.File;
import java.io.RandomAccessFile;



public class HashExtensivel implements Indexador {

    private static final int  TAM_BUCKET  = 10;
    private static final int  TAM_ENTRADA = Integer.BYTES + Long.BYTES; // 4 + 8 = 12
    private static final long NULO        = -1L;

    private final RandomAccessFile dir;
    private final RandomAccessFile bucket;
    private int profGlobal;

    public HashExtensivel(String nomeBase) throws Exception {
        new File("./data/idx").mkdirs();

        this.dir    = new RandomAccessFile("./data/idx/" + nomeBase + ".dir.bin", "rw");
        this.bucket = new RandomAccessFile("./data/idx/" + nomeBase + ".bkt.bin", "rw");

        if (this.dir.length() == 0) {
            // Inicialização: profundidade 1, dois buckets vazios
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

    // Indexador 

    
    public void inserir(int chave, long enderecoNoArquivo) throws Exception {
        inserirNoBucket(chave, enderecoNoArquivo);
    }

    
    public long buscar(int chave) throws Exception {
        long endBkt = getEnderecoDoBucket(hash(chave));
        return buscarNoBucket(endBkt, chave);
    }

    
    public boolean remover(int chave) throws Exception {
        long endBkt = getEnderecoDoBucket(hash(chave));
        return removerDoBucket(endBkt, chave);
    }

    
    public boolean atualizar(int chave, long novoEndereco) throws Exception {
        long endBkt = getEnderecoDoBucket(hash(chave));
        return atualizarNoBucket(endBkt, chave, novoEndereco);
    }

    
    public long getTotalChaves() throws Exception {
        int capacidade = 1 << profGlobal;
        long total = 0;
        long visitado = -1; // evita contar buckets compartilhados duas vezes

        for (int i = 0; i < capacidade; i++) {
            long endBkt = getEnderecoDoBucket(i);
            // Buckets podem ser compartilhados (mesmo ponteiro em entradas distintas)
            if (endBkt != visitado) {
                total   += getCount(endBkt);
                visitado = endBkt;
            }
        }
        return total;
    }


    
    public long[][] listarOrdenado() throws Exception {
        int capacidade = 1 << profGlobal;
        java.util.List<long[]> lista = new java.util.ArrayList<>();
        long ultimoBkt = -1;

        for (int i = 0; i < capacidade; i++) {
            long endBkt = getEnderecoDoBucket(i);
            if (endBkt == ultimoBkt) continue; // bucket compartilhado — pula
            ultimoBkt = endBkt;

            int  count   = getCount(endBkt);
            long posBase = endBkt + (2L * Integer.BYTES);

            for (int j = 0; j < count; j++) {
                this.bucket.seek(posBase + (long) j * TAM_ENTRADA);
                int  chave    = this.bucket.readInt();
                long endereco = this.bucket.readLong();
                lista.add(new long[]{ chave, endereco });
            }
        }
        return lista.toArray(new long[0][]);
    }

    @Override
    public void close() throws Exception {
        this.dir.close();
        this.bucket.close();
    }

    
    // Lógica interna


    private int hash(int chave) {
        return chave & ((1 << this.profGlobal) - 1);
    }

    private long getEnderecoDoBucket(int index) throws Exception {
        this.dir.seek(Integer.BYTES + (long) index * Long.BYTES);
        return this.dir.readLong();
    }

    private void setEnderecoDoBucket(int idx, long endereco) throws Exception {
        this.dir.seek(Integer.BYTES + (long) idx * Long.BYTES);
        this.dir.writeLong(endereco);
    }

    private boolean inserirNoBucket(int chave, long valor) throws Exception {
        int  idxDir = hash(chave);
        long endBkt = getEnderecoDoBucket(idxDir);

        // Se a chave já existe, apenas atualiza o offset (idempotente)
        if (atualizarNoBucket(endBkt, chave, valor)) return true;

        int count = getCount(endBkt);
        if (count < TAM_BUCKET) {
            adicionarEntrada(endBkt, chave, valor);
            return true;
        }

        // Bucket cheio — divide
        int profLocal = getProfLocal(endBkt);
        if (profLocal == this.profGlobal) dobrarDiretorio();
        splitBucket(endBkt, profLocal);
        return inserirNoBucket(chave, valor); // tenta novamente após a divisão
    }

    private void dobrarDiretorio() throws Exception {
        int    cap      = 1 << this.profGlobal;
        long[] ptrs     = new long[cap];
        for (int i = 0; i < cap; i++) ptrs[i] = getEnderecoDoBucket(i);

        this.profGlobal++;
        int novaCap = 1 << this.profGlobal;

        this.dir.seek(0);
        this.dir.writeInt(this.profGlobal);
        for (int i = 0; i < novaCap; i++) this.dir.writeLong(ptrs[i % cap]);
    }

    private void splitBucket(long endBktAntigo, int profLocal) throws Exception {
        int  novaProfLocal = profLocal + 1;
        int[]  chaves      = new int[TAM_BUCKET];
        long[] enderecos   = new long[TAM_BUCKET];
        lerEntradas(endBktAntigo, chaves, enderecos);

        long endBktNovo = criarBucket(novaProfLocal);

        // Zera o bucket antigo com a nova profundidade local
        this.bucket.seek(endBktAntigo);
        this.bucket.writeInt(novaProfLocal);
        this.bucket.writeInt(0);
        for (int i = 0; i < TAM_BUCKET; i++) {
            this.bucket.writeInt(0);
            this.bucket.writeLong(NULO);
        }

        // Redireciona entradas do diretório que apontam para o bucket antigo
        int tamanho = 1 << this.profGlobal;
        for (int i = 0; i < tamanho; i++) {
            if (getEnderecoDoBucket(i) == endBktAntigo) {
                if (((i >> profLocal) & 1) == 1) setEnderecoDoBucket(i, endBktNovo);
            }
        }

        // Reinsere os pares nos buckets corretos
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
            this.bucket.writeInt(0);
            this.bucket.writeLong(NULO);
        }
        return pos;
    }

    private void adicionarEntrada(long endBkt, int chave, long valor) throws Exception {
        int  count = getCount(endBkt);
        long pos   = endBkt + (2L * Integer.BYTES) + (long) count * TAM_ENTRADA;
        this.bucket.seek(pos);
        this.bucket.writeInt(chave);
        this.bucket.writeLong(valor);
        setCount(endBkt, count + 1);
    }

    private long buscarNoBucket(long endBkt, int chave) throws Exception {
        int  count   = getCount(endBkt);
        long posBase = endBkt + (2L * Integer.BYTES);
        for (int i = 0; i < count; i++) {
            this.bucket.seek(posBase + (long) i * TAM_ENTRADA);
            if (this.bucket.readInt() == chave) return this.bucket.readLong();
        }
        return NULO;
    }

    private boolean removerDoBucket(long endBkt, int chave) throws Exception {
        int  count   = getCount(endBkt);
        long posBase = endBkt + (2L * Integer.BYTES);
        for (int i = 0; i < count; i++) {
            long pos = posBase + (long) i * TAM_ENTRADA;
            this.bucket.seek(pos);
            if (this.bucket.readInt() == chave) {
                // Compacta: move o último par para a posição removida
                if (i < count - 1) {
                    long ulPos = posBase + (long)(count - 1) * TAM_ENTRADA;
                    this.bucket.seek(ulPos);
                    int  kU = this.bucket.readInt();
                    long vU = this.bucket.readLong();
                    this.bucket.seek(pos);
                    this.bucket.writeInt(kU);
                    this.bucket.writeLong(vU);
                }
                setCount(endBkt, count - 1);
                return true;
            }
        }
        return false;
    }

    private boolean atualizarNoBucket(long endBkt, int chave, long novoValor) throws Exception {
        int  count   = getCount(endBkt);
        long posBase = endBkt + (2L * Integer.BYTES);
        for (int i = 0; i < count; i++) {
            long pos = posBase + (long) i * TAM_ENTRADA;
            this.bucket.seek(pos);
            if (this.bucket.readInt() == chave) {
                this.bucket.writeLong(novoValor);
                return true;
            }
        }
        return false;
    }

    private void lerEntradas(long endBkt, int[] chaves, long[] enderecos) throws Exception {
        int  count   = getCount(endBkt);
        long posBase = endBkt + (2L * Integer.BYTES);
        for (int i = 0; i < TAM_BUCKET; i++) { chaves[i] = 0; enderecos[i] = NULO; }
        for (int i = 0; i < count; i++) {
            this.bucket.seek(posBase + (long) i * TAM_ENTRADA);
            chaves[i]    = this.bucket.readInt();
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