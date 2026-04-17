package src.util;

import java.io.File;
import java.io.RandomAccessFile;


public class HashExtensivel implements Indexador{
    private static final int TAM_BUCKET = 10;
    private static final int TAM_ENTRADA = Integer.BYTES + Long.BYTES;
    private static final long NULO = -1L;

    private final RandomAccessFile dir;
    private final RandomAccessFile bucket;


    private int profGlobal;

    public HashExtensivel(String nomeBase) throws Exception{
        new File("./data/hashExtensivelFiles").mkdirs();

        this.dir = new RandomAccessFile("./data/hashExtensivelFiles/" + nomeBase + ".dir.bin", "rw");
        this.bucket = new RandomAccessFile("./data/hashExtensivelFiles/" + nomeBase + ".bkt.bin", "rw");

        if(this.dir.length() == 0){
            this.profGlobal = 1;
            this.dir.writeInt(this.profGlobal);
            long end0 = criarBucket(1);
            long end1 = criarBucket(1);

            this.dir.writeLong(end0);
            this.dir.writeLong(end1);
        }else{
            this.profGlobal = this.dir.readInt();
        }
    }


    public void inserir(int chave, long enderecoNoArquivo) throws Exception{
        inserirNoBucket(chave, enderecoNoArquivo);
    }

    public long buscar(int chave) throws Exception{
        long endBkt = getEnderecoDoBucket(hash(chave));
        return buscarNoBucket(endBkt, chave);
    }

    public boolean remover(int chave) throws Exception{
        long endBkt = getEnderecoDoBucket(hash(chave));
        return removerDoBucket(endBkt, chave);
    }

    public boolean atualizar(int chave, long novoEndereco) throws Exception {
        long endBkt = getEnderecoDoBucket(hash(chave));
        return atualizarNoBucket(endBkt, chave, novoEndereco);
    }

    private int hash(int chave) {
        return chave & ((1 << this.profGlobal) - 1);
    }

    private long getEnderecoDoBucket(int index) throws Exception {
        this.dir.seek(Integer.BYTES + (long) index * Long.BYTES);
        return this.dir.readLong();
    }

    private boolean inserirNoBucket(int chave, long valor) throws Exception{
        int diretorioIndex = hash(chave);
        long endBkt = getEnderecoDoBucket(diretorioIndex);
        
        if(atualizarNoBucket(endBkt, chave, valor))return true;

        int count = getCount(endBkt);
        if(count < TAM_BUCKET){
            adicionarEntrada(endBkt, chave, valor);
            return true;
        }

        int profLocal = getProfLocal(endBkt);
        if(profLocal == this.profGlobal)dobrarDiretório();

        splitBucket(endBkt, profLocal);
        return inserirNoBucket(chave, valor);
    }

    private void dobrarDiretório() throws Exception{
        int capacidadeDiretorio = 1 << this.profGlobal;
        long[] ponteiros = new long[capacidadeDiretorio];

        for(int i = 0; i < capacidadeDiretorio; i++)ponteiros[i] = getEnderecoDoBucket(i);

        this.profGlobal++;
        int novaCapacidade = 1 << this.profGlobal;

        this.dir.seek(0);
        this.dir.writeInt(this.profGlobal);

        for(int i = 0; i < novaCapacidade; i++)this.dir.writeLong(ponteiros[i % capacidadeDiretorio]);
    }

    private void splitBucket(long endBktAntigo, int profLocal) throws Exception{
        int novaProfLocal = profLocal + 1;

        int[] chaves = new int[TAM_BUCKET];
        long[] enderecos = new long[TAM_BUCKET];
        lerEntradas(endBktAntigo, chaves, enderecos);

        long endBktNovo = criarBucket(novaProfLocal);

        this.dir.seek(endBktAntigo);
        this.dir.writeInt(novaProfLocal);
        this.dir.writeInt(0);

        for(int i = 0; i < TAM_BUCKET; i++){
            this.dir.writeInt(0);
            this.bucket.writeLong(NULO);
        }

        int tamanho = 1 << this.profGlobal;
        for(int i = 0; i < tamanho; i++){
            if(getEnderecoDoBucket(i) == endBktAntigo){
                if(((i >> profLocal) & 1) == 1)setEndBucket(i, endBktNovo);
            }
        }

        for(int i = 0; i < TAM_BUCKET; i++){
            if(enderecos[i] != NULO)inserirNoBucket(chaves[i], enderecos[i]);
        }
    }

    private long criarBucket(int profLocal) throws Exception{
        long enderecoInicial = this.bucket.length();
        this.bucket.seek(enderecoInicial);

        this.bucket.writeInt(profLocal);
        this.bucket.writeInt(0); //quantidade de elementos dentro do bucket

        for(int i = 0; i < TAM_BUCKET; i++){
            //apenas preenchendo o bucket com lixo para garantir o espaço para as entradas
            this.bucket.writeInt(0);
            this.bucket.writeLong(NULO);
        }

        return enderecoInicial;
    }

    private void adicionarEntrada(long endBkt, int chave, long valor) throws Exception {
        int count = getCount(endBkt);

        long pos = endBkt + (Integer.BYTES * 2) + (long) count * TAM_ENTRADA;
        this.bucket.seek(pos);
        this.bucket.writeInt(chave);
        this.bucket.writeLong(valor);
        setCount(endBkt, count + 1);
    }

    private long buscarNoBucket(long endBkt, int chave) throws Exception {
        int count = getCount(endBkt);
        long posBase = endBkt + (Integer.BYTES * 2);

        for (int i = 0; i < count; i++) {
            this.bucket.seek(posBase + (long) i * TAM_ENTRADA);
            int  key = this.bucket.readInt();
            long endereco = this.bucket.readLong();
            if (key == chave) return endereco;
        }
        return NULO;
    }

    private boolean removerDoBucket(long endBkt, int chave) throws Exception {
        int count = getCount(endBkt);
        long posBase = endBkt + (Integer.BYTES * 2);

        for (int i = 0; i < count; i++) {
            long pos = posBase + (long) i * TAM_ENTRADA;
            this.bucket.seek(pos);
            int key = this.bucket.readInt();

            if (key == chave) {
                if (i < count - 1) {
                    long ultimaPos = posBase + (long)(count - 1) * TAM_ENTRADA;
                    this.bucket.seek(ultimaPos);
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
        int count = getCount(endBkt);
        long posicaoInicial = endBkt + (Integer.BYTES * 2);

        for (int i = 0; i < count; i++) {
            long pos = posicaoInicial + (long) i * TAM_ENTRADA;
            this.bucket.seek(pos);
            int key = this.bucket.readInt();
            if (key == chave) {
                this.bucket.writeLong(novoValor);
                return true;
            }
        }
        return false;
    }

    private void lerEntradas(long endBkt, int[] chaves, long[] enderecos) throws Exception {
        int count = getCount(endBkt);
        long posBase = endBkt + (2 * Integer.BYTES);

        for (int i = 0; i < TAM_BUCKET; i++) {
            chaves[i]  = 0;
            enderecos[i] = NULO;
        }

        for (int i = 0; i < count; i++) {
            this.bucket.seek(posBase + (long) i * TAM_ENTRADA);
            chaves[i]  = this.bucket.readInt();
            enderecos[i] = this.bucket.readLong();
        }
    }

    private void setEndBucket(int idx, long endereco) throws Exception {
        this.dir.seek(Integer.BYTES + (long) idx * Long.BYTES);
        this.dir.writeLong(endereco);
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

    public void close() throws Exception {
        this.dir.close();
        this.bucket.close();
    }
}
