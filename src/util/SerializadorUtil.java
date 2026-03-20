package src.util;

import java.io.*;

public class SerializadorUtil {

    private static final char DELIMITADOR = ';';

    // --- String ---

    public static void writeString(DataOutputStream dos, String valor) throws IOException {
        if (valor == null) valor = "";
        dos.write(valor.getBytes("UTF-8"));
        dos.writeByte(DELIMITADOR);
    }

    public static String readString(DataInputStream dis) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        int b;

        while ((b = dis.read()) != -1 && b != DELIMITADOR) {
            buffer.write(b);
        }

        return buffer.toString("UTF-8");
    }

    // --- ISBN ---

    public static void writeIsbn(DataOutputStream dos, char[] isbn) throws IOException {
        byte[] bytes = new byte[13];

        if (isbn != null) {
            for (int i = 0; i < 13 && i < isbn.length; i++) {
                bytes[i] = (byte) isbn[i];
            }
        }

        dos.write(bytes);
    }

    public static char[] readIsbn(DataInputStream dis) throws IOException {
        byte[] bytes = new byte[13];

        dis.readFully(bytes);

        char[] isbn = new char[13];

        for (int i = 0; i < 13; i++) {
            isbn[i] = (char) bytes[i];
        }

        return isbn;
    }

    // --- String[] ---

    public static void writeStringArray(DataOutputStream dos, String[] valores) throws IOException {
        if (valores == null) { 
            dos.writeInt(0); return;
        }

        dos.writeInt(valores.length);

        for (String s : valores) { 
            writeString(dos, s);
        }
    }

    public static String[] readStringArray(DataInputStream dis) throws IOException {
        int qtd = dis.readInt();
        String[] valores = new String[qtd];

        for (int i = 0; i < qtd; i++) { 
            valores[i] = readString(dis);
        }

        return valores;
    }

    // --- int[] ---

    public static void writeIntArray(DataOutputStream dos, int[] valores) throws IOException {
        if (valores == null) { 
            dos.writeInt(0); return; 
        }

        dos.writeInt(valores.length);

        for (int v : valores) { 
            dos.writeInt(v);
        }
    }

    public static int[] readIntArray(DataInputStream dis) throws IOException {
        int qtd = dis.readInt();
        int[] valores = new int[qtd];

        for (int i = 0; i < qtd; i++) { 
            valores[i] = dis.readInt();
        }

        return valores;
    }
}