import src.dao.DAOFactory;
import src.server.ApiServer;
import src.compression.*;
import src.util.PacoteBackup;
import java.io.*;
import java.nio.file.*;
import java.util.Arrays;

public class Main {

    private static final int    PORT     = 8080;
    private static final String WEB_ROOT = "./web";
    private static final String DATA_DIR = "./data";
    private static final String BACKUP_DIR = "./backups";

    public static void main(String[] args) {
        if (args.length > 0) {
            if (args[0].equals("--backup")) {
                executarBackupCompleto();
                return;
            } else if (args[0].equals("--restore") && args.length > 1) {
                executarRestore(args[1]);
                return;
            }
        }
        iniciarServidor();
    }
    
    private static void executarBackupCompleto() {
        System.out.println("=== MODO BACKUP - FASE IV ===\n");
        
        try {
            Files.createDirectories(Paths.get(BACKUP_DIR));
            
            long tamanhoTotal = PacoteBackup.calcularTamanhoDiretorio(DATA_DIR);
            System.out.printf("Tamanho total do diretório ./data: %,d bytes (%.2f KB)%n%n", tamanhoTotal, tamanhoTotal / 1024.0);
            
            System.out.println("Empacotando arquivos...");
            byte[] pacote = PacoteBackup.empacotar(DATA_DIR);
            System.out.printf("Pacote criado: %,d bytes%n%n", pacote.length);
            
            System.out.println("--- COMPRESSÃO HUFFMAN ---");
            CompressionMetrics metricsHuffman = testarCompressao(pacote, "huffman", "backup_huffman.bin");
            metricsHuffman.exibirRelatorio("HUFFMAN");
            
            System.out.println("\n--- COMPRESSÃO LZW ---");
            CompressionMetrics metricsLZW = testarCompressao(pacote, "lzw", "backup_lzw.bin");
            metricsLZW.exibirRelatorio("LZW");
            
            gerarRelatorioComparativo(metricsHuffman, metricsLZW);
            
        } catch (Exception e) {
            System.err.println("Erro durante backup: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static CompressionMetrics testarCompressao(byte[] dados, String algoritmo, String nomeArquivo) throws IOException {
        long startComp = System.currentTimeMillis();
        byte[] comprimido;
        if ("huffman".equals(algoritmo)) {
            comprimido = HuffmanCompressor.comprimir(dados);
        } else {
            comprimido = LZWCompressor.comprimir(dados);
        }
        long compTime = System.currentTimeMillis() - startComp;
        
        Path arquivoBackup = Paths.get(BACKUP_DIR, nomeArquivo);
        Files.write(arquivoBackup, comprimido);
        System.out.printf("Arquivo salvo: %s (%,d bytes)%n", arquivoBackup, comprimido.length);
        
        long startDecomp = System.currentTimeMillis();
        byte[] descomprimido;
        if ("huffman".equals(algoritmo)) {
            descomprimido = HuffmanCompressor.descomprimir(comprimido);
        } else {
            descomprimido = LZWCompressor.descomprimir(comprimido);
        }
        long decompTime = System.currentTimeMillis() - startDecomp;
        
        if (dados.length != descomprimido.length) {
            System.err.println("ERRO: Tamanho não corresponde!");
        } else if (!Arrays.equals(dados, descomprimido)) {
            System.err.println("ERRO: Conteúdo corrompido!");
        } else {
            System.out.println("✓ Verificação de integridade: OK");
        }
        
        return new CompressionMetrics(dados.length, comprimido.length, compTime, decompTime);
    }
    
    private static void executarRestore(String arquivoBackup) {
        System.out.println("=== MODO RESTORE ===\n");
        
        try {
            Path backupPath = Paths.get(arquivoBackup);
            if (!Files.exists(backupPath)) {
                System.err.println("❌ Arquivo não encontrado: " + arquivoBackup);
                return;
            }
            
            System.out.println("📁 Arquivo de backup: " + backupPath.toAbsolutePath());
            
            byte[] comprimido = Files.readAllBytes(backupPath);
            byte[] pacote;
            
            if (arquivoBackup.contains("huffman")) {
                System.out.println("🔐 Detectado: Backup Huffman");
                pacote = HuffmanCompressor.descomprimir(comprimido);
            } else if (arquivoBackup.contains("lzw")) {
                System.out.println("🔐 Detectado: Backup LZW");
                pacote = LZWCompressor.descomprimir(comprimido);
            } else {
                System.err.println("❌ Tipo de backup não reconhecido.");
                return;
            }
            
            System.out.println("📦 Tamanho do pacote descomprimido: " + pacote.length + " bytes");
            
            if (pacote.length <= 18) {
                System.err.println("❌ Pacote vazio! Nada para restaurar.");
                return;
            }
            
            String timestamp = new java.text.SimpleDateFormat("yyyyMMdd_HHmmss")
                                    .format(new java.util.Date());
            
            Path dataPath = Paths.get(DATA_DIR);
            
            if (Files.exists(dataPath) && Files.isDirectory(dataPath)) {
                Path backupAtual = Paths.get(BACKUP_DIR, "data_backup_" + timestamp);
                System.out.println("📦 Criando backup da pasta ./data atual em: " + backupAtual);
                Files.move(dataPath, backupAtual, StandardCopyOption.REPLACE_EXISTING);
                System.out.println("✅ Backup da pasta data criado!");
            }
            
            Files.createDirectories(Paths.get(DATA_DIR));
            Files.createDirectories(Paths.get(DATA_DIR + "/idx"));
            
            System.out.println("🔄 Restaurando dados...");
            PacoteBackup.desempacotar(pacote, ".");
            
            if (Files.exists(Paths.get(DATA_DIR))) {
                long totalBytes = calcularTamanhoDiretorio(DATA_DIR);
                System.out.println("✅ Pasta ./data restaurada com sucesso!");
                System.out.println("📊 Tamanho total restaurado: " + formatarBytes(totalBytes));
            } else {
                System.err.println("⚠ ATENÇÃO: Pasta ./data não foi encontrada após restore!");
            }
            
            System.out.println("\n✅ Restore concluído!");
            
        } catch (Exception e) {
            System.err.println("❌ Erro durante restore: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void gerarRelatorioComparativo(CompressionMetrics huffman, CompressionMetrics lzw) {
        System.out.println("\n=== RELATÓRIO COMPARATIVO FINAL ===");
        System.out.println("+----------------+----------------+----------------+");
        System.out.println("|     Métrica    |     Huffman    |       LZW      |");
        System.out.println("+----------------+----------------+----------------+");
        System.out.printf("| Tamanho Final   | %12d B | %12d B |%n",
            huffman.getTamanhoComprimido(), lzw.getTamanhoComprimido());
        System.out.printf("| Taxa Compressão | %13.2f%% | %13.2f%% |%n",
            huffman.getTaxaCompressao(), lzw.getTaxaCompressao());
        System.out.printf("| Fator          | %13.2f:1 | %13.2f:1 |%n",
            huffman.getFatorCompressao(), lzw.getFatorCompressao());
        System.out.println("+----------------+----------------+----------------+");
    }
    
    private static long calcularTamanhoDiretorio(String dirPath) throws IOException {
        Path root = Paths.get(dirPath);
        if (!Files.exists(root)) return 0;
        return Files.walk(root)
                .filter(Files::isRegularFile)
                .mapToLong(p -> {
                    try { return Files.size(p); }
                    catch (IOException e) { return 0; }
                })
                .sum();
    }
    
    private static String formatarBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.2f KB", bytes / 1024.0);
        return String.format("%.2f MB", bytes / (1024.0 * 1024.0));
    }
    
    private static void iniciarServidor() {
        try {
            DAOFactory factory = new DAOFactory();
            System.out.println("DAOFactory inicializada com sucesso.");

            ApiServer server = new ApiServer(PORT, factory, WEB_ROOT);
            server.start();

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("\nEncerrando processos do BiblioSys...");
                System.out.println("Sistema encerrado.");
            }));

            System.out.println("Servidor ativo: http://localhost:" + PORT);
            System.out.println("Pressione [ENTER] no terminal para interromper a execucao.");
            System.out.println("\nPara fazer backup: java Main --backup");
            System.out.println("Para restaurar:   java Main --restore backups/backup_huffman.bin");

            System.in.read();
            System.out.println("Interrupcao solicitada pelo usuario.");
            System.exit(0);

        } catch (Exception e) {
            System.err.println("Erro fatal durante a inicializacao do sistema:");
            e.printStackTrace();
            System.exit(1);
        }
    }
}