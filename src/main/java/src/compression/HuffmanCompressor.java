package src.compression;

import java.io.*;
import java.util.*;

public class HuffmanCompressor {
    
    private static final int MAGIC_NUMBER = 0x4846464D; // "HFFM"
    private static final int VERSAO = 1;
    
    public static byte[] comprimir(byte[] dados) throws IOException {
        if (dados == null || dados.length == 0) return new byte[0];
        
        int[] frequencias = new int[256];
        for (byte b : dados) frequencias[b & 0xFF]++;
        
        HuffmanNode raiz = construirArvore(frequencias);
        String[] codigos = new String[256];
        gerarCodigos(raiz, "", codigos);
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        BitOutputStream bos = new BitOutputStream(baos);
        
        // Cabeçalho
        baos.write(MAGIC_NUMBER >> 24);
        baos.write(MAGIC_NUMBER >> 16);
        baos.write(MAGIC_NUMBER >> 8);
        baos.write(MAGIC_NUMBER);
        baos.write(VERSAO);
        
        // Tamanho original (8 bytes)
        long tamanhoOriginal = dados.length;
        for (int i = 56; i >= 0; i -= 8) {
            baos.write((int)((tamanhoOriginal >> i) & 0xFF));
        }
        
        serializarArvore(raiz, baos);
        
        for (byte b : dados) {
            String codigo = codigos[b & 0xFF];
            for (char c : codigo.toCharArray()) {
                bos.writeBit(c == '1' ? 1 : 0);
            }
        }
        bos.flush();
        
        return baos.toByteArray();
    }
    
    public static byte[] descomprimir(byte[] comprimido) throws IOException {
        if (comprimido == null || comprimido.length < 16) {
            throw new IOException("Dados comprimidos inválidos");
        }
        
        ByteArrayInputStream bais = new ByteArrayInputStream(comprimido);
        BitInputStream bis = new BitInputStream(bais);
        
        int magic = (bais.read() << 24) | (bais.read() << 16) | 
                    (bais.read() << 8) | bais.read();
        if (magic != MAGIC_NUMBER) throw new IOException("Formato Huffman inválido");
        
        bais.read(); // versão
        
        long tamanhoOriginal = 0;
        for (int i = 0; i < 8; i++) tamanhoOriginal = (tamanhoOriginal << 8) | (bais.read() & 0xFF);
        
        HuffmanNode raiz = desserializarArvore(bais);
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream((int) tamanhoOriginal);
        HuffmanNode atual = raiz;
        
        int bit;
        while (baos.size() < tamanhoOriginal && (bit = bis.readBit()) != -1) {
            atual = (bit == 0) ? atual.esquerda : atual.direita;
            if (atual.isFolha()) {
                baos.write(atual.valor);
                atual = raiz;
            }
        }
        
        return baos.toByteArray();
    }
    
    private static HuffmanNode construirArvore(int[] freq) {
        PriorityQueue<HuffmanNode> pq = new PriorityQueue<>();
        for (int i = 0; i < 256; i++) if (freq[i] > 0) pq.offer(new HuffmanNode((byte) i, freq[i]));
        if (pq.size() == 1) { HuffmanNode unico = pq.poll(); return new HuffmanNode(unico.frequencia, unico, null); }
        while (pq.size() > 1) {
            HuffmanNode esq = pq.poll(), dir = pq.poll();
            pq.offer(new HuffmanNode(esq.frequencia + dir.frequencia, esq, dir));
        }
        return pq.poll();
    }
    
    private static void gerarCodigos(HuffmanNode node, String codigo, String[] codigos) {
        if (node == null) return;
        if (node.isFolha()) { codigos[node.valor & 0xFF] = codigo; return; }
        gerarCodigos(node.esquerda, codigo + "0", codigos);
        gerarCodigos(node.direita, codigo + "1", codigos);
    }
    
    private static void serializarArvore(HuffmanNode node, OutputStream out) throws IOException {
        if (node.isFolha()) { out.write(1); out.write(node.valor); }
        else { out.write(0); serializarArvore(node.esquerda, out); serializarArvore(node.direita, out); }
    }
    
    private static HuffmanNode desserializarArvore(InputStream in) throws IOException {
        int marker = in.read();
        if (marker == -1) return null;
        if (marker == 1) return new HuffmanNode((byte) in.read(), 0);
        HuffmanNode esq = desserializarArvore(in);
        HuffmanNode dir = desserializarArvore(in);
        return new HuffmanNode(0, esq, dir);
    }
}