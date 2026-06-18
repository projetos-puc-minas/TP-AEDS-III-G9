package src.search;

import java.util.ArrayList;
import java.util.List;

/**
 * Implementação do algoritmo Knuth-Morris-Pratt (KMP) para casamento de padrões.
 * Complexidade O(n + m) onde n = tamanho do texto, m = tamanho do padrão.
 */
public class KMP {

    /**
     * Busca todas as ocorrências do padrão no texto.
     * @param texto  texto a ser percorrido (pode ser o título, sinopse, etc.)
     * @param padrao padrão a ser encontrado
     * @return lista de posições (índices) onde o padrão ocorre no texto
     */
    public static List<Integer> buscar(String texto, String padrao) {
        List<Integer> ocorrencias = new ArrayList<>();
        if (texto == null || padrao == null || padrao.isEmpty()) {
            return ocorrencias;
        }

        // Para busca case-insensitive, converte ambos para lower case
        String text = texto.toLowerCase();
        String pat = padrao.toLowerCase();

        int[] lps = preprocessar(pat);
        int i = 0; // índice no texto
        int j = 0; // índice no padrão

        while (i < text.length()) {
            if (pat.charAt(j) == text.charAt(i)) {
                i++;
                j++;
                if (j == pat.length()) {
                    ocorrencias.add(i - j);
                    j = lps[j - 1];
                }
            } else {
                if (j != 0) {
                    j = lps[j - 1];
                } else {
                    i++;
                }
            }
        }
        return ocorrencias;
    }

    /**
     * Pré-processa o padrão para construir o vetor LPS (Longest Proper Prefix which is also Suffix).
     */
    private static int[] preprocessar(String padrao) {
        int m = padrao.length();
        int[] lps = new int[m];
        int len = 0; // comprimento do prefixo/sufixo mais longo anterior
        int i = 1;
        while (i < m) {
            if (padrao.charAt(i) == padrao.charAt(len)) {
                len++;
                lps[i] = len;
                i++;
            } else {
                if (len != 0) {
                    len = lps[len - 1];
                } else {
                    lps[i] = 0;
                    i++;
                }
            }
        }
        return lps;
    }
}