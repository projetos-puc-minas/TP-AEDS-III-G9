package src.compression;

import java.io.*;

public class BitInputStream implements AutoCloseable {
    private InputStream in;
    private int buffer;
    private int bitsNoBuffer;
    
    public BitInputStream(InputStream in) {
        this.in = in;
        this.buffer = 0;
        this.bitsNoBuffer = 0;
    }
    
    public int readBit() throws IOException {
        if (bitsNoBuffer == 0) {
            buffer = in.read();
            if (buffer == -1) return -1;
            bitsNoBuffer = 8;
        }
        bitsNoBuffer--;
        return (buffer >> bitsNoBuffer) & 1;
    }
    
    public int readBits(int numBits) throws IOException {
        int valor = 0;
        for (int i = numBits - 1; i >= 0; i--) {
            int bit = readBit();
            if (bit == -1) return -1;
            valor = (valor << 1) | bit;
        }
        return valor;
    }
    
    @Override
    public void close() throws IOException {
        in.close();
    }
}