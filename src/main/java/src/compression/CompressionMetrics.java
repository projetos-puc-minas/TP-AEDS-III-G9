package src.compression;

public class CompressionMetrics {
    
    private final long tamanhoOriginal;
    private final long tamanhoComprimido;
    private final double taxaCompressao;
    private final double fatorCompressao;
    private final long tempoCompressaoMs;
    private final long tempoDescompressaoMs;
    
    public CompressionMetrics(long original, long comprimido, long tempoComp, long tempoDescomp) {
        this.tamanhoOriginal = original;
        this.tamanhoComprimido = comprimido;
        this.tempoCompressaoMs = tempoComp;
        this.tempoDescompressaoMs = tempoDescomp;
        if (original > 0) {
            this.taxaCompressao = ((original - comprimido) / (double) original) * 100;
            this.fatorCompressao = original / (double) comprimido;
        } else {
            this.taxaCompressao = 0;
            this.fatorCompressao = 1;
        }
    }
    
    public void exibirRelatorio(String algoritmo) {
        String corVerde = "\u001B[32m";
        String corAzul = "\u001B[34m";
        String corAmarelo = "\u001B[33m";
        String corCiano = "\u001B[36m";
        String corMagenta = "\u001B[35m";
        String corVermelho = "\u001B[31m";
        String corReset = "\u001B[0m";
        String negrito = "\u001B[1m";
        
        // Cabeçalho com moldura
        System.out.println();
        System.out.println(corCiano + "╔══════════════════════════════════════════════════════════════════╗" + corReset);
        System.out.println(corCiano + "║" + negrito + "                    RELATÓRIO DE COMPRESSÃO - " + algoritmo + "                    " + corReset + corCiano + "║" + corReset);
        System.out.println(corCiano + "╚══════════════════════════════════════════════════════════════════╝" + corReset);
        
        System.out.println();
        
        // Tabela de métricas
        System.out.println(corAzul + "┌─────────────────────────────────────────────────────────────────┐" + corReset);
        System.out.println(corAzul + "│" + negrito + "                         MÉTRICAS PRINCIPAIS                        " + negrito + corAzul + "│" + corReset);
        System.out.println(corAzul + "├─────────────────────────────────────────────────────────────────┤" + corReset);
        
        System.out.printf(corAzul + "│" + corReset + " %-20s : %,12d bytes (%,8.2f KB)         " + corAzul + "│%n" + corReset,
            "Tamanho original", tamanhoOriginal, tamanhoOriginal / 1024.0);
        
        System.out.printf(corAzul + "│" + corReset + " %-20s : %,12d bytes (%,8.2f KB)         " + corAzul + "│%n" + corReset,
            "Tamanho comprimido", tamanhoComprimido, tamanhoComprimido / 1024.0);
        
        System.out.println(corAzul + "├─────────────────────────────────────────────────────────────────┤" + corReset);
        
        // Taxa e fator com cores especiais
        System.out.printf(corAzul + "│" + corReset + " %-20s : " + corAmarelo + "%15.2f%%" + corReset + "                           " + corAzul + "│%n",
            "Taxa de compressão", taxaCompressao);
        
        System.out.printf(corAzul + "│" + corReset + " %-20s : " + corAmarelo + "%15.2f:1" + corReset + "                           " + corAzul + "│%n",
            "Fator de compressão", fatorCompressao);
        
        System.out.println(corAzul + "├─────────────────────────────────────────────────────────────────┤" + corReset);
        
        System.out.printf(corAzul + "│" + corReset + " %-20s : %12d ms                           " + corAzul + "│%n",
            "Tempo de compressão", tempoCompressaoMs);
        
        System.out.printf(corAzul + "│" + corReset + " %-20s : %12d ms                           " + corAzul + "│%n",
            "Tempo de descompressão", tempoDescompressaoMs);
        
        System.out.println(corAzul + "└─────────────────────────────────────────────────────────────────┘" + corReset);
        
        // Barra de compressão visual
        System.out.println();
        exibirBarraCompressao();
        
        // Interpretação
        System.out.println();
        System.out.println(corCiano + "┌─────────────────────────────────────────────────────────────────┐" + corReset);
        System.out.println(corCiano + "│" + negrito + "                         INTERPRETAÇÃO                           " + negrito + corCiano + "│" + corReset);
        System.out.println(corCiano + "├─────────────────────────────────────────────────────────────────┤" + corReset);
        
        String icone = "";
        String mensagem = "";
        
        if (taxaCompressao > 50) {
            icone = corVerde + "✓✓✓" + corReset;
            mensagem = "EXCELENTE compressão - redução de mais da metade do tamanho!";
        } else if (taxaCompressao > 30) {
            icone = corVerde + "✓✓" + corReset;
            mensagem = "BOA compressão - redução significativa de espaço";
        } else if (taxaCompressao > 15) {
            icone = corAmarelo + "✓" + corReset;
            mensagem = "COMPRESSÃO MODERADA - economia de espaço razoável";
        } else if (taxaCompressao > 5) {
            icone = corAmarelo + "~" + corReset;
            mensagem = "COMPRESSÃO LEVE - pequena redução de espaço";
        } else {
            icone = corVermelho + "✗" + corReset;
            mensagem = "COMPRESSÃO LIMITADA - dados com pouca redundância";
        }
        
        System.out.printf(corAzul + "│" + corReset + " %-10s : %-50s " + corAzul + "│%n", "Classificação", icone + " " + mensagem);
        
        if (fatorCompressao > 2.5) {
            System.out.printf(corAzul + "│" + corReset + " %-10s : %-50s " + corAzul + "│%n", "Análise", corVerde + "Cada 1 byte comprimido = " + String.format("%.2f", fatorCompressao) + " bytes originais" + corReset);
        } else if (fatorCompressao > 1.5) {
            System.out.printf(corAzul + "│" + corReset + " %-10s : %-50s " + corAzul + "│%n", "Análise", corAmarelo + "Cada 1 byte comprimido = " + String.format("%.2f", fatorCompressao) + " bytes originais" + corReset);
        } else {
            System.out.printf(corAzul + "│" + corReset + " %-10s : %-50s " + corAzul + "│%n", "Análise", "Cada 1 byte comprimido = " + String.format("%.2f", fatorCompressao) + " bytes originais");
        }
        
        System.out.println(corCiano + "└─────────────────────────────────────────────────────────────────┘" + corReset);
    }
    
    private void exibirBarraCompressao() {
        int tamanhoBarra = 50;
        int espacosSalvos = (int) (taxaCompressao / 100 * tamanhoBarra);
        int espacosRestantes = tamanhoBarra - espacosSalvos;
        
        String corVerde = "\u001B[32m";
        String corCinza = "\u001B[90m";
        String corReset = "\u001B[0m";
        
        System.out.print("  " + corVerde + "█" + corReset.repeat(Math.max(0, espacosSalvos)));
        System.out.print(corCinza + "░" + corReset.repeat(Math.max(0, espacosRestantes)));
        System.out.printf("  %5.1f%% comprimido%n", taxaCompressao);
        
        // Segunda barra mostrando o fator
        int fatorEscala = (int) Math.min(fatorCompressao, 5) * 10;
        System.out.print("  " + corVerde + "█" + corReset.repeat(Math.max(0, fatorEscala)));
        System.out.print(corCinza + "░" + corReset.repeat(Math.max(0, 50 - fatorEscala)));
        System.out.printf("  Fator %.2f:1%n", fatorCompressao);
    }
    
    public void exibirComparativo(CompressionMetrics outro, String nomeAlgoritmo2) {
        String corVerde = "\u001B[32m";
        String corVermelho = "\u001B[31m";
        String corAmarelo = "\u001B[33m";
        String corCiano = "\u001B[36m";
        String corReset = "\u001B[0m";
        String negrito = "\u001B[1m";
        
        System.out.println();
        System.out.println(corCiano + "╔════════════════════════════════════════════════════════════════════════════════════════╗" + corReset);
        System.out.println(corCiano + "║" + negrito + "                                   COMPARAÇÃO ENTRE ALGORITMOS                                    " + negrito + corCiano + "║" + corReset);
        System.out.println(corCiano + "╚════════════════════════════════════════════════════════════════════════════════════════╝" + corReset);
        
        System.out.println();
        System.out.printf("  %-20s │ %15s │ %15s │ %10s%n", "Métrica", "Huffman", nomeAlgoritmo2, "Vencedor");
        System.out.println("  " + "─".repeat(70));
        
        // Comparar taxas
        String vencedorTaxa = this.taxaCompressao > outro.taxaCompressao ? "Huffman ✓" : nomeAlgoritmo2 + " ✓";
        String corTaxa = this.taxaCompressao > outro.taxaCompressao ? corVerde : corAmarelo;
        System.out.printf("  %-20s │ %14.2f%% │ %14.2f%% │ %s%s%s%n", 
            "Taxa de compressão", this.taxaCompressao, outro.taxaCompressao, corTaxa, vencedorTaxa, corReset);
        
        // Comparar fatores
        String vencedorFator = this.fatorCompressao > outro.fatorCompressao ? "Huffman ✓" : nomeAlgoritmo2 + " ✓";
        String corFator = this.fatorCompressao > outro.fatorCompressao ? corVerde : corAmarelo;
        System.out.printf("  %-20s │ %14.2f:1 │ %14.2f:1 │ %s%s%s%n", 
            "Fator de compressão", this.fatorCompressao, outro.fatorCompressao, corFator, vencedorFator, corReset);
        
        // Comparar tempos de compressão
        String vencedorComp = this.tempoCompressaoMs < outro.tempoCompressaoMs ? "Huffman ✓" : nomeAlgoritmo2 + " ✓";
        String corComp = this.tempoCompressaoMs < outro.tempoCompressaoMs ? corVerde : corAmarelo;
        System.out.printf("  %-20s │ %14d ms │ %14d ms │ %s%s%s%n", 
            "Tempo compressão", this.tempoCompressaoMs, outro.tempoCompressaoMs, corComp, vencedorComp, corReset);
        
        // Comparar tempos de descompressão
        String vencedorDescomp = this.tempoDescompressaoMs < outro.tempoDescompressaoMs ? "Huffman ✓" : nomeAlgoritmo2 + " ✓";
        String corDescomp = this.tempoDescompressaoMs < outro.tempoDescompressaoMs ? corVerde : corAmarelo;
        System.out.printf("  %-20s │ %14d ms │ %14d ms │ %s%s%s%n", 
            "Tempo descompressão", this.tempoDescompressaoMs, outro.tempoDescompressaoMs, corDescomp, vencedorDescomp, corReset);
        
        System.out.println();
        
        // Resumo final
        System.out.println(corCiano + "╔════════════════════════════════════════════════════════════════════════════════════════╗" + corReset);
        System.out.println(corCiano + "║" + negrito + "                                      RECOMENDAÇÃO FINAL                                      " + negrito + corCiano + "║" + corReset);
        System.out.println(corCiano + "╚════════════════════════════════════════════════════════════════════════════════════════╝" + corReset);
        
        if (this.taxaCompressao > outro.taxaCompressao && this.tempoCompressaoMs < outro.tempoCompressaoMs) {
            System.out.println(corVerde + "  ✓ Huffman é superior em TODOS os aspectos (mais compressão e mais rápido!)" + corReset);
        } else if (outro.taxaCompressao > this.taxaCompressao && outro.tempoCompressaoMs < this.tempoCompressaoMs) {
            System.out.println(corVerde + "  ✓ " + nomeAlgoritmo2 + " é superior em TODOS os aspectos (mais compressão e mais rápido!)" + corReset);
        } else if (this.taxaCompressao > outro.taxaCompressao) {
            System.out.println(corAmarelo + "  ⚠ Huffman comprime melhor, mas é mais lento." + corReset);
            System.out.println(corAmarelo + "  ✓ Para backups frequentes: prefira Huffman (rápido)" + corReset);
            System.out.println(corVerde + "  ✓ Para economia de espaço: prefira " + nomeAlgoritmo2 + " (melhor compressão)" + corReset);
        } else {
            System.out.println(corAmarelo + "  ⚠ " + nomeAlgoritmo2 + " comprime melhor, mas é mais lento." + corReset);
            System.out.println(corVerde + "  ✓ Para economia de espaço: prefira " + nomeAlgoritmo2 + " (melhor compressão)" + corReset);
            System.out.println(corAmarelo + "  ✓ Para backups frequentes: prefira Huffman (mais rápido)" + corReset);
        }
        
        System.out.println();
    }
    
    public long getTamanhoOriginal() { return tamanhoOriginal; }
    public long getTamanhoComprimido() { return tamanhoComprimido; }
    public double getTaxaCompressao() { return taxaCompressao; }
    public double getFatorCompressao() { return fatorCompressao; }
}