package src.compression;

public class HuffmanNode implements Comparable<HuffmanNode> {
    byte valor;
    int frequencia;
    HuffmanNode esquerda;
    HuffmanNode direita;
    
    public HuffmanNode(byte valor, int freq) {
        this.valor = valor;
        this.frequencia = freq;
    }
    
    public HuffmanNode(int freq, HuffmanNode esq, HuffmanNode dir) {
        this.frequencia = freq;
        this.esquerda = esq;
        this.direita = dir;
    }
    
    public boolean isFolha() {
        return esquerda == null && direita == null;
    }
    
    @Override
    public int compareTo(HuffmanNode o) {
        return Integer.compare(this.frequencia, o.frequencia);
    }
}