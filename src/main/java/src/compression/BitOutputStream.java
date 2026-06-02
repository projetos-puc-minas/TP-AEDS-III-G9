package src.compression;

import java.io.*;

public class BitOutputStream implements AutoCloseable {
    private OutputStream out;
    private int buffer;
    private int bitsNoBuffer;
    
    public BitOutputStream(OutputStream out) {
        this.out = out;
        this.buffer = 0;
        this.bitsNoBuffer = 0;
    }
    
    public void writeBit(int bit) throws IOException {
        if (bit != 0 && bit != 1) {
            throw new IllegalArgumentException("Bit deve ser 0 ou 1");
        }
        buffer = (buffer << 1) | bit;
        bitsNoBuffer++;
        
        if (bitsNoBuffer == 8) {
            flush();
        }
    }
    
    public void writeBits(int valor, int numBits) throws IOException {
        for (int i = numBits - 1; i >= 0; i--) {
            writeBit((valor >> i) & 1);
        }
    }
    
    public void flush() throws IOException {
        if (bitsNoBuffer > 0) {
            buffer <<= (8 - bitsNoBuffer);
            out.write(buffer);
            buffer = 0;
            bitsNoBuffer = 0;
        }
    }
    
    @Override
    public void close() throws IOException {
        flush();
        out.close();
    }
}