package src.util;

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
    }
}