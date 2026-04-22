package src.util;

<<<<<<< HEAD:src/util/OrdenacaoExterna.java
import java.io.File;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;

public class OrdenacaoExterna
{
    public static final int tamCabecalho = 16;
    public static final int blocos = 10;
    public static final String diretorioTemp = "./data/temp/";

    public void ordenarPorTitulo(String caminhoArquivo) throws Exception
    {

        // crio uma pasta temporaria se ela nao existir
        File diretorio = new File(diretorioTemp);

        if (!diretorio.exists()) {
            diretorio.mkdir();
        }

        System.out.println("Distribuição: ");
        List<File> arquivosTemporarios = faseDistribuicao(caminhoArquivo);

        System.out.println("Intercalação...");
        File arquivoOrdenado = faseIntercalacao(arquivosTemporarios);

        substituirArquivoOriginal(caminhoArquivo, arquivoOrdenado);
        System.out.println("Ordenação concluída");
    }

    private List<File> faseDistribuicao(String caminhoArquivo) throws Exception
    {
        List<File> arquivosTemp = new ArrayList<>();
        RandomAccessFile arqOriginal = new RandomAccessFile(caminhoArquivo, "r");

        // pulo o cabçalho
        arqOriginal.seek(tamCabecalho);

        // Cria o bloco que vai guardar 10 livros
        List<Livro> blocoRAM = new ArrayList<>();

        // so pra nomear os blocos bonitinhos
        int contadorArquivos = 0;

        // ler o arquivo inteiro
        while (arqOriginal.getFilePointer() < arqOriginal.length())
        {

            boolean lapide = arqOriginal.readBoolean();
            int tamRegistro = arqOriginal.readInt();

            // crio um vetor de bytes com o tamanho do livro
            byte[] dados = new byte[tamRegistro];

            // Leio a quantidade exata de bytes do arquivo e preenchemos o nosso vetor 'dados'.
            arqOriginal.readFully(dados);

            // se o arquivo nao foi "excluido" jogo ele no bloco
            if (lapide) {
                Livro livro = new Livro();
                livro.fromByteArray(dados);
                blocoRAM.add(livro);
            }

            // verifica se o bloco ta cheio ou se acabou o arquivo
            if (blocoRAM.size() == blocos || arqOriginal.getFilePointer() == arqOriginal.length())
            {
                // verifica se a lista esta vazia
                if (!blocoRAM.isEmpty())
                {

                    // ordeno o livro 1 e 2 pelos seus titulos ignorando letras maiusculas e minusculas
                    blocoRAM.sort((l1, l2) -> l1.getTitulo().compareToIgnoreCase(l2.getTitulo()));

                    File temp = new File(diretorioTemp + "temp_" + contadorArquivos + ".bin");

                    // Chama a função auxiliar que pega a lista ordenada e salva ela dentro do arquivo no HD.
                    salvarBlocoEmArquivo(temp, blocoRAM);
                    arquivosTemp.add(temp);

                    // Esvazia a memoria para que possa ler os próximos 10 livros.
                    blocoRAM.clear();
                    contadorArquivos++;
                }
            }
        } // fim while

        arqOriginal.close();

        // Devolve a lista contendo todos os arquivos temporários gerados.
        return arquivosTemp;
    }

    // Função que recebe um arquivo vazio e uma lista de livros, e transforma tudo em bytes no HD.
    private void salvarBlocoEmArquivo(File arquivo, List<Livro> bloco) throws Exception
    {

        RandomAccessFile arqTemp = new RandomAccessFile(arquivo, "rw");

        for (Livro l : bloco)
        {
            // transforma os atributos dos livros em bytes
            byte[] dados = l.toByteArray();

            // Escreve a lápide como 'true'
            arqTemp.writeBoolean(true);

            // Escreve o tamanho do vetor de bytes que vem a seguir.
            arqTemp.writeInt(dados.length);

            // Escreve os bytes reais do livro no arquivo.
            arqTemp.write(dados);
        }

        // Fecha o arquivo temporário.
        arqTemp.close();
    }

    // INTERCALAÇÃO
    private File faseIntercalacao(List<File> arquivosTemp) throws Exception
    {

        // deixar o nome bonitinho
        int geracao = 0;

        while (arquivosTemp.size() > 1)
        {

            List<File> novosArquivos = new ArrayList<>();

            // Pula de 2 em 2 para formar os pares
            for (int i = 0; i < arquivosTemp.size(); i += 2)
            {

                File arq1 = arquivosTemp.get(i);

                // Verifica se o arquivo tem mais uma posição
                if (i + 1 < arquivosTemp.size())
                {
                    // Pega o segundo arquivo
                    File arq2 = arquivosTemp.get(i + 1);

                    File arqMerge = new File(diretorioTemp + "merge_" + geracao + "_" + i + ".bin");

                    // Faz a fusão dos dois e anota o vencedor na lista nova
                    fazerMerge(arq1, arq2, arqMerge);
                    novosArquivos.add(arqMerge);

                    arq1.delete();
                    arq2.delete();
                }
                else
                {
                    // se nao tem proximo ele e o utimo ent nao tem que intercalar os arquivos e so jogar ele no arquivo
                    novosArquivos.add(arq1);
                }
            }

            arquivosTemp = novosArquivos;

            // sobrescevr o nome
            geracao++;
        }

        // O último arquivo que sobrar na lista é o grande campeão ordenado
        return arquivosTemp.get(0);
    }

    // funcao para intercalar os vetores
    private void fazerMerge(File f1, File f2, File fOut) throws Exception
    {

        RandomAccessFile arq1 = new RandomAccessFile(f1, "r");
        RandomAccessFile arq2 = new RandomAccessFile(f2, "r");

        RandomAccessFile arqOut = new RandomAccessFile(fOut, "rw");

        Livro l1 = lerProximoLivro(arq1);
        Livro l2 = lerProximoLivro(arq2);

        while (l1 != null && l2 != null) {

            if (l1.getTitulo().compareToIgnoreCase(l2.getTitulo()) <= 0)
            {
                escreverLivro(arqOut, l1);
                l1 = lerProximoLivro(arq1);
            } else {
                escreverLivro(arqOut, l2);
                l2 = lerProximoLivro(arq2);
            }
        }

        // Se o arquivo 2 acabar copia tudo oq esta dentro do 1
        while (l1 != null) 
        {
            escreverLivro(arqOut, l1);
            l1 = lerProximoLivro(arq1);
        }

        // Contrario do de cima
        while (l2 != null)
        {
            escreverLivro(arqOut, l2);
            l2 = lerProximoLivro(arq2);
        }

        arq1.close();
        arq2.close();
        arqOut.close();
    }

    // AUXILIARES
    private Livro lerProximoLivro(RandomAccessFile arq) throws Exception
    {
    while (arq.getFilePointer() < arq.length())
    {
        boolean lapide = arq.readBoolean();
        int tamRegistro = arq.readInt();
        byte[] dados = new byte[tamRegistro];
        arq.readFully(dados);

        if (lapide)
        {
            Livro l = new Livro();
            l.fromByteArray(dados);
            return l;
        }
        // se a lapide for falsa so ignora ela
    }
    return null;
}

    private void escreverLivro(RandomAccessFile arq, Livro l) throws Exception
    {
        byte[] dados = l.toByteArray();
        arq.writeBoolean(true);
        arq.writeInt(dados.length);
        arq.write(dados);
    }

    private void substituirArquivoOriginal(String originalPath, File novoArquivoTemp) throws Exception
    {

        RandomAccessFile arqOriginalAntigo = new RandomAccessFile(originalPath, "r");
        RandomAccessFile arqNovoFinal = new RandomAccessFile(originalPath + ".novo", "rw");

        arqOriginalAntigo.seek(0);

        byte[] cabecalho = new byte[tamCabecalho];

        arqOriginalAntigo.readFully(cabecalho);
        arqNovoFinal.write(cabecalho);
        arqOriginalAntigo.close();

        RandomAccessFile tempOrdenado = new RandomAccessFile(novoArquivoTemp, "r");

        byte[] buffer = new byte[4096];
        int lidos;

        while ((lidos = tempOrdenado.read(buffer)) != -1) {
            arqNovoFinal.write(buffer, 0, lidos);
        }

        tempOrdenado.close();
        arqNovoFinal.close();

        File fVelho = new File(originalPath);
        File fNovo = new File(originalPath + ".novo");

        fVelho.delete();
        fNovo.renameTo(fVelho);

        novoArquivoTemp.delete();
=======
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

    private final Arquivo<T>     arquivo;
    private final Comparator<T>  comparador;
    private final Constructor<T> construtor;

    /**
     * @param arquivo    instância do Arquivo<T> já aberto (fonte dos dados)
     * @param construtor construtor padrão de T (para desserialização)
     * @param comparador define o atributo de ordenação (ex: por id, titulo, ano)
     */
    public OrdenacaoExterna(Arquivo<T> arquivo, Constructor<T> construtor, Comparator<T> comparador) {
        this.arquivo    = arquivo;
        this.construtor = construtor;
        this.comparador = comparador;
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

            // Ordena o bloco em memória usando o comparador fornecido
            bloco.sort((a, b) -> comparador.compare(a.objeto, b.objeto));

            File tmp = File.createTempFile("bibliosys_run_", ".tmp");
            tmp.deleteOnExit();

            try (DataOutputStream dos = new DataOutputStream(
                    new BufferedOutputStream(new FileOutputStream(tmp)))) {
                dos.writeInt(bloco.size());
                for (Arquivo.OffsetEntry<T> entry : bloco) {
                    dos.writeInt(entry.objeto.getId()); // chave
                    dos.writeLong(entry.offset);         // offset no .bin
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
     *
     * CORREÇÃO: a comparação usa o mesmo comparador da 1ª passagem,
     * relendo os objetos do arquivo principal pelo offset armazenado no run.
     * Isso garante que a ordem do run final respeite o atributo de ordenação
     * (título, nome, etc.) e não apenas o ID.
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

            int[]  idA  = new int[1];  long[] offA = new long[1];
            int[]  idB  = new int[1];  long[] offB = new long[1];
            boolean temA = lerPar(disA, idA, offA);
            boolean temB = lerPar(disB, idB, offB);

            while (temA && temB) {
                // Relê os objetos pelo offset para comparar pelo atributo correto
                T objA = arquivo.readByOffset(offA[0]);
                T objB = arquivo.readByOffset(offB[0]);

                // Fallback para comparação por ID caso o objeto tenha sido excluído
                // entre a geração do run e esta passagem (situação rara mas defensiva)
                int cmp = (objA != null && objB != null)
                        ? comparador.compare(objA, objB)
                        : Integer.compare(idA[0], idB[0]);

                if (cmp <= 0) {
                    dos.writeInt(idA[0]); dos.writeLong(offA[0]);
                    temA = lerPar(disA, idA, offA);
                } else {
                    dos.writeInt(idB[0]); dos.writeLong(offB[0]);
                    temB = lerPar(disB, idB, offB);
                }
            }

            // Esgota o que sobrou de cada run
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
                pares[i][0] = dis.readInt();  // id
                pares[i][1] = dis.readLong(); // offset
            }
            run.delete();
            return pares;
        }
>>>>>>> luisa/livros:src/main/java/src/util/OrdenacaoExterna.java
    }
}