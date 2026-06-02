package src.util;

import java.io.*;
import java.nio.file.*;
import java.util.*;

public class PacoteBackup {
    
    public static byte[] empacotar(String dirPath) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        
        dos.writeInt(0x424B5054); // "BKPT"
        dos.writeShort(1);
        
        Path root = Paths.get(dirPath);
        List<Path> arquivos = new ArrayList<>();
        if (Files.exists(root)) {
            Files.walk(root).filter(Files::isRegularFile).forEach(arquivos::add);
        }
        
        dos.writeInt(arquivos.size());
        
        for (Path arq : arquivos) {
            String relativePath = root.relativize(arq).toString().replace('\\', '/');
            byte[] conteudo = Files.readAllBytes(arq);
            
            byte[] nomeBytes = relativePath.getBytes("UTF-8");
            dos.writeInt(nomeBytes.length);
            dos.write(nomeBytes);
            dos.writeLong(conteudo.length);
            dos.write(conteudo);
        }
        
        dos.writeLong(-1L);
        return baos.toByteArray();
    }
    
    public static void desempacotar(byte[] pacote, String outputDir) throws IOException {
        DataInputStream dis = new DataInputStream(new ByteArrayInputStream(pacote));
        
        int magic = dis.readInt();
        if (magic != 0x424B5054) throw new IOException("Formato de pacote inválido");
        
        short versao = dis.readShort();
        if (versao != 1) throw new IOException("Versão não suportada");
        
        int qtdArquivos = dis.readInt();
        Path root = Paths.get(outputDir);
        
        for (int i = 0; i < qtdArquivos; i++) {
            int nomeLen = dis.readInt();
            byte[] nomeBytes = new byte[nomeLen];
            dis.readFully(nomeBytes);
            String nomeArq = new String(nomeBytes, "UTF-8");
            
            long tamanho = dis.readLong();
            byte[] conteudo = new byte[(int) tamanho];
            dis.readFully(conteudo);
            
            Path destino = root.resolve(nomeArq);
            Files.createDirectories(destino.getParent());
            Files.write(destino, conteudo);
        }
    }
    
    public static long calcularTamanhoDiretorio(String dirPath) throws IOException {
        Path root = Paths.get(dirPath);
        if (!Files.exists(root)) return 0;
        return Files.walk(root).filter(Files::isRegularFile).mapToLong(p -> {
            try { return Files.size(p); } catch (IOException e) { return 0; }
        }).sum();
    }
}