package src.util;

import java.io.*;
import java.lang.reflect.Constructor;
import java.util.*;

/**
 * Ordenação Externa por Intercalação (External Merge Sort).
 *
 * Implementada conforme o enunciado (Fase 2, item 3d):
 *   1ª passagem — gera "runs" ordenados em arquivos temporários usando memória limitada.
 *   2ª passagem — intercala os runs em pares até restar um único arquivo ordenado.
 *
 * O resultado é uma lista de pares [chave, offset] prontos para popular a ArvoreBMais.
 * Não altera o arquivo .bin original — opera apenas em arquivos temporários.
 *
 * @param <T> Tipo do registro (deve implementar Registro e ser Comparable por atributo)
 */
public class OrdenacaoExterna<T extends Registro> {

    // Número máximo de registros mantidos em RAM por vez durante a geração de runs
    private static final int TAM_BLOCO = 8;

    private final Arquivo<T>    arquivo;
    private final Comparator<T> comparador;
    private final Constructor<T> construtor;

    /**
     * @param arquivo    instância do Arquivo<T> já aberto (fonte dos dados)
     * @param construtor construtor padrão de T (para desserialização)
     * @param comparador define o atributo de ordenação (ex: por id, titulo, ano)
     */
    public OrdenacaoExterna(Arquivo<T> arquivo, Constructor<T> construtor, Comparator<T> comparador) {
        this.arquivo     = arquivo;
        this.construtor  = construtor;
        this.comparador  = comparador;
    }

    /**
     * Executa a ordenação e retorna os pares [chave (id), offset físico] na ordem definida
     * pelo comparador. A ArvoreBMais pode ser populada diretamente com esse resultado.
     *
     * @return matriz long[n][2] onde cada linha é { id, offsetNoArquivoBin }
     */
    public long[][] ordenar() throws Exception {
        // 1ª passagem: gera runs ordenados em disco
        List<File> runs = gerarRuns();
        if (runs.isEmpty()) return new long[0][2];

        // 2ª passagem: intercala par a par até sobrar um único run
        while (runs.size() > 1) {
            runs = intercalar(runs);
        }

        // Lê o run final e devolve os pares [id, offset]
        return lerRunFinal(runs.get(0));
    }

    // -------------------------------------------------------------------------
    // 1ª Passagem — geração de runs
    // -------------------------------------------------------------------------

    private List<File> gerarRuns() throws Exception {
        List<Arquivo.OffsetEntry<T>> todos = arquivo.listarComOffset();
        List<File> runs = new ArrayList<>();
        int i = 0;

        while (i < todos.size()) {
            int fim = Math.min(i + TAM_BLOCO, todos.size());
            List<Arquivo.OffsetEntry<T>> bloco = new ArrayList<>(todos.subList(i, fim));

            // Ordena o bloco em memória
            bloco.sort((a, b) -> comparador.compare(a.objeto, b.objeto));

            File tmp = File.createTempFile("bibliosys_run_", ".tmp");
            tmp.deleteOnExit();

            try (DataOutputStream dos = new DataOutputStream(
                    new BufferedOutputStream(new FileOutputStream(tmp)))) {
                dos.writeInt(bloco.size());
                for (Arquivo.OffsetEntry<T> entry : bloco) {
                    dos.writeInt(entry.objeto.getId());   // chave
                    dos.writeLong(entry.offset);           // offset no .bin
                }
            }

            runs.add(tmp);
            i = fim;
        }

        return runs;
    }

    // -------------------------------------------------------------------------
    // 2ª Passagem — intercalação par a par
    // -------------------------------------------------------------------------

    private List<File> intercalar(List<File> runs) throws Exception {
        List<File> resultado = new ArrayList<>();
        int i = 0;

        while (i < runs.size()) {
            if (i + 1 >= runs.size()) {
                // Run ímpar: passa direto para a próxima rodada
                resultado.add(runs.get(i));
                i++;
            } else {
                File merged = intercalarDois(runs.get(i), runs.get(i + 1));
                runs.get(i).delete();
                runs.get(i + 1).delete();
                resultado.add(merged);
                i += 2;
            }
        }

        return resultado;
    }

    /**
     * Intercala dois runs ordenados em um único run ordenado.
     * Lê um par de cada vez de cada run — sem carregar tudo na memória.
     */
    private File intercalarDois(File runA, File runB) throws Exception {
        File saida = File.createTempFile("bibliosys_merge_", ".tmp");
        saida.deleteOnExit();

        try (DataInputStream disA = new DataInputStream(new BufferedInputStream(new FileInputStream(runA)));
             DataInputStream disB = new DataInputStream(new BufferedInputStream(new FileInputStream(runB)));
             DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(saida)))) {

            int tamA = disA.readInt();
            int tamB = disB.readInt();
            dos.writeInt(tamA + tamB); // tamanho do run resultante

            // Lê o primeiro par de cada run
            int[]  idA     = new int[1];  long[] offA = new long[1];
            int[]  idB     = new int[1];  long[] offB = new long[1];
            boolean temA = lerPar(disA, idA, offA);
            boolean temB = lerPar(disB, idB, offB);

            // Intercalação clássica por comparação de chave
            while (temA && temB) {
                if (idA[0] <= idB[0]) {
                    dos.writeInt(idA[0]); dos.writeLong(offA[0]);
                    temA = lerPar(disA, idA, offA);
                } else {
                    dos.writeInt(idB[0]); dos.writeLong(offB[0]);
                    temB = lerPar(disB, idB, offB);
                }
            }

            // Esgota o que sobrou
            while (temA) { dos.writeInt(idA[0]); dos.writeLong(offA[0]); temA = lerPar(disA, idA, offA); }
            while (temB) { dos.writeInt(idB[0]); dos.writeLong(offB[0]); temB = lerPar(disB, idB, offB); }
        }

        return saida;
    }

    private boolean lerPar(DataInputStream dis, int[] id, long[] off) {
        try {
            id[0]  = dis.readInt();
            off[0] = dis.readLong();
            return true;
        } catch (EOFException e) {
            return false;
        } catch (IOException e) {
            return false;
        }
    }

    // -------------------------------------------------------------------------
    // Leitura do run final
    // -------------------------------------------------------------------------

    private long[][] lerRunFinal(File run) throws Exception {
        try (DataInputStream dis = new DataInputStream(
                new BufferedInputStream(new FileInputStream(run)))) {
            int n = dis.readInt();
            long[][] pares = new long[n][2];
            for (int i = 0; i < n; i++) {
                pares[i][0] = dis.readInt();   // id
                pares[i][1] = dis.readLong();  // offset
            }
            run.delete();
            return pares;
        }
    }
}