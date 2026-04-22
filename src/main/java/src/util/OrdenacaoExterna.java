package src.util;

import java.io.File;
import java.io.RandomAccessFile;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;


public class OrdenacaoExterna<T extends Registro> {

    // Quantidade máxima de registros carregados na RAM por bloco
    public static final int    RAM_BLOCOS = 10;
    public static final String DIR_TEMP   = "./data/temp/";

    private final Arquivo<T>     arquivo;
    private final Constructor<T> construtor;
    private final Comparator<T>  comparador;

    // Construtor

    /**
     * @param arquivo     Instância do Arquivo<T> a ser ordenado.
     * @param construtor  Construtor padrão (sem argumentos) da entidade T.
     * @param comparador  Critério de ordenação para T.
     */
    public OrdenacaoExterna(Arquivo<T>     arquivo,
                            Constructor<T> construtor,
                            Comparator<T>  comparador) {
        this.arquivo    = arquivo;
        this.construtor = construtor;
        this.comparador = comparador;
    }

    // Ponto de Entrada Principal

    public long[][] ordenar() throws Exception {

        // Garante que o diretório temporário existe
        File dir = new File(DIR_TEMP);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        List<File> arquivosTemp = faseDistribuicao();
        if (arquivosTemp.isEmpty()) {
            return new long[0][2];
        }

        File arquivoFinal = faseIntercalacao(arquivosTemp);

        // Lê o arquivo final ordenado e monta o array de pares de offsets
        long[][] pares = lerPares(arquivoFinal);

        // Limpa o arquivo final temporário
        arquivoFinal.delete();

        return pares;
    }

    // Fase 1 — Distribuição

    private List<File> faseDistribuicao() throws Exception {

        List<File>   arquivosTemp  = new ArrayList<>();
        List<T>      blocoRAM      = new ArrayList<>();
        List<long[]> offsetsBloco  = new ArrayList<>(); // offsets correspondentes a cada objeto
        int          contadorBlocos = 0;

        // Itera sobre todos os registros ativos com seus offsets físicos
        for (Arquivo.OffsetEntry<T> entrada : arquivo.listarComOffset()) {
            blocoRAM.add(entrada.objeto);
            offsetsBloco.add(new long[]{ entrada.offset });

            // Bloco cheio — ordena e grava no disco
            if (blocoRAM.size() == RAM_BLOCOS) {
                contadorBlocos = gravarBlocoOrdenado(
                    blocoRAM, offsetsBloco, arquivosTemp, contadorBlocos);
            }
        }

        // Último bloco parcial (pode ser menor que RAM_BLOCOS)
        if (!blocoRAM.isEmpty()) {
            contadorBlocos = gravarBlocoOrdenado(
                blocoRAM, offsetsBloco, arquivosTemp, contadorBlocos);
        }

        return arquivosTemp;
    }


    private int gravarBlocoOrdenado(List<T>      blocoRAM,
                                    List<long[]> offsetsBloco,
                                    List<File>   arquivosTemp,
                                    int          contadorBlocos) throws Exception {

        // Cria uma lista de índices e os ordena pelo comparador sem mover os objetos
        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < blocoRAM.size(); i++) {
            indices.add(i);
        }
        indices.sort((ia, ib) -> comparador.compare(blocoRAM.get(ia), blocoRAM.get(ib)));

        File temp = new File(DIR_TEMP + "temp_" + contadorBlocos + ".bin");

        try (RandomAccessFile raf = new RandomAccessFile(temp, "rw")) {
            for (int idx : indices) {
                raf.writeLong(offsetsBloco.get(idx)[0]);
            }
        }

        arquivosTemp.add(temp);
        blocoRAM.clear();
        offsetsBloco.clear();

        return contadorBlocos + 1;
    }

    // Fase 2 — Intercalação
    private File faseIntercalacao(List<File> arquivosTemp) throws Exception {

        int geracao = 0;

        while (arquivosTemp.size() > 1) {

            List<File> novosArquivos = new ArrayList<>();

            // Percorre de 2 em 2 para formar os pares de intercalação
            for (int i = 0; i < arquivosTemp.size(); i += 2) {

                File arq1 = arquivosTemp.get(i);

                if (i + 1 < arquivosTemp.size()) {
                    File arq2     = arquivosTemp.get(i + 1);
                    File arqMerge = new File(DIR_TEMP + "merge_" + geracao + "_" + i + ".bin");

                    // Funde os dois arquivos ordenados num único
                    fazerMerge(arq1, arq2, arqMerge);
                    novosArquivos.add(arqMerge);

                    arq1.delete();
                    arq2.delete();
                } else {
                    // Arquivo ímpar — passa direto para a próxima geração sem intercalar
                    novosArquivos.add(arq1);
                }
            }

            arquivosTemp = novosArquivos;
            geracao++;
        }

        // O único arquivo restante é o resultado final ordenado
        return arquivosTemp.get(0);
    }

    private void fazerMerge(File f1, File f2, File fOut) throws Exception {

        try (RandomAccessFile arq1   = new RandomAccessFile(f1,   "r");
             RandomAccessFile arq2   = new RandomAccessFile(f2,   "r");
             RandomAccessFile arqOut = new RandomAccessFile(fOut, "rw")) {

            Long off1 = lerProximoOffset(arq1);
            Long off2 = lerProximoOffset(arq2);

            while (off1 != null && off2 != null) {

                T obj1 = arquivo.readByOffset(off1);
                T obj2 = arquivo.readByOffset(off2);

                if (comparador.compare(obj1, obj2) <= 0) {
                    arqOut.writeLong(off1);
                    off1 = lerProximoOffset(arq1);
                } else {
                    arqOut.writeLong(off2);
                    off2 = lerProximoOffset(arq2);
                }
            }

            // Esgota o restante do arquivo 1
            while (off1 != null) {
                arqOut.writeLong(off1);
                off1 = lerProximoOffset(arq1);
            }

            // Esgota o restante do arquivo 2
            while (off2 != null) {
                arqOut.writeLong(off2);
                off2 = lerProximoOffset(arq2);
            }
        }
    }


    // Leitura do Arquivo Final e Montagem dos Pares

    private long[][] lerPares(File arquivoFinal) throws Exception {

        List<Long> offsets = new ArrayList<>();

        try (RandomAccessFile raf = new RandomAccessFile(arquivoFinal, "r")) {
            while (raf.getFilePointer() < raf.length()) {
                offsets.add(raf.readLong());
            }
        }

        long[][] pares = new long[offsets.size()][2];
        for (int i = 0; i < offsets.size(); i++) {
            pares[i][0] = i;              // posição na sequência ordenada
            pares[i][1] = offsets.get(i); // offset físico no arquivo principal
        }
        return pares;
    }

    // Auxiliares

    private Long lerProximoOffset(RandomAccessFile arq) throws Exception {
        if (arq.getFilePointer() < arq.length()) {
            return arq.readLong();
        }
        return null;
    }
}