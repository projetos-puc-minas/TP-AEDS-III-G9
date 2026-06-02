package src.compression;

import java.io.*;
import java.util.*;

public class LZWCompressor {
    
    private static final int MAGIC_NUMBER = 0x4C5A5700;
    private static final int VERSAO = 1;
    private static final int MAX_TABLE_SIZE = 4096;
    private static final int CODE_SIZE = 12;
    
    public static byte[] comprimir(byte[] dados) throws IOException {
        if (dados == null || dados.length == 0) return new byte[0];
        
        Map<String, Integer> dicionario = new HashMap<>();
        for (int i = 0; i < 256; i++) dicionario.put(String.valueOf((char) i), i);
        int proximoCodigo = 256;
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        BitOutputStream bos = new BitOutputStream(baos);
        
        baos.write(MAGIC_NUMBER >> 24); baos.write(MAGIC_NUMBER >> 16);
        baos.write(MAGIC_NUMBER >> 8); baos.write(MAGIC_NUMBER);
        baos.write(VERSAO);
        
        long tamanhoOriginal = dados.length;
        for (int i = 56; i >= 0; i -= 8) baos.write((int)((tamanhoOriginal >> i) & 0xFF));
        
        StringBuilder buffer = new StringBuilder();
        for (byte b : dados) {
            String simbolo = String.valueOf((char) (b & 0xFF));
            String bufferSimbolo = buffer.toString() + simbolo;
            if (dicionario.containsKey(bufferSimbolo)) buffer.append(simbolo);
            else {
                bos.writeBits(dicionario.get(buffer.toString()), CODE_SIZE);
                if (proximoCodigo < MAX_TABLE_SIZE) dicionario.put(bufferSimbolo, proximoCodigo++);
                buffer = new StringBuilder(simbolo);
            }
        }
        if (buffer.length() > 0) bos.writeBits(dicionario.get(buffer.toString()), CODE_SIZE);
        bos.flush();
        return baos.toByteArray();
    }
    
    public static byte[] descomprimir(byte[] comprimido) throws IOException {
        if (comprimido == null || comprimido.length < 16) throw new IOException("Dados comprimidos inválidos");
        
        ByteArrayInputStream bais = new ByteArrayInputStream(comprimido);
        BitInputStream bis = new BitInputStream(bais);
        
        int magic = (bais.read() << 24) | (bais.read() << 16) | (bais.read() << 8) | bais.read();
        if (magic != MAGIC_NUMBER) throw new IOException("Formato LZW inválido");
        bais.read(); // versão
        
        long tamanhoOriginal = 0;
        for (int i = 0; i < 8; i++) tamanhoOriginal = (tamanhoOriginal << 8) | (bais.read() & 0xFF);
        
        Map<Integer, String> dicionario = new HashMap<>();
        for (int i = 0; i < 256; i++) dicionario.put(i, String.valueOf((char) i));
        int proximoCodigo = 256;
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream((int) tamanhoOriginal);
        
        int codigoAnterior = bis.readBits(CODE_SIZE);
        if (codigoAnterior == -1) throw new IOException("Arquivo comprimido vazio");
        
        String saidaAnterior = dicionario.get(codigoAnterior);
        for (char c : saidaAnterior.toCharArray()) baos.write((byte) c);
        
        int codigoAtual;
        while ((codigoAtual = bis.readBits(CODE_SIZE)) != -1) {
            String entrada;
            if (dicionario.containsKey(codigoAtual)) entrada = dicionario.get(codigoAtual);
            else if (codigoAtual == proximoCodigo) entrada = saidaAnterior + saidaAnterior.charAt(0);
            else throw new IOException("Código LZW inválido: " + codigoAtual);
            
            for (char c : entrada.toCharArray()) baos.write((byte) c);
            if (proximoCodigo < MAX_TABLE_SIZE) dicionario.put(proximoCodigo++, saidaAnterior + entrada.charAt(0));
            saidaAnterior = entrada;
        }
        return baos.toByteArray();
    }
}