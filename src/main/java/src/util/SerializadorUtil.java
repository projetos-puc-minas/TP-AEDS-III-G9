package src.util;

import java.io.*;

/**
 * Utilitários de serialização para arquivos binários.
 *
 * PADRÃO ADOTADO (único em todo o projeto):
 *   - String  → bytes UTF-8 + 1 byte delimitador ';'   (compatível com docs)
 *   - char[13] (ISBN) → 13 bytes fixos
 *   - String[] → 4 bytes (quantidade) + cada String no padrão acima
 *   - int[]   → 4 bytes (quantidade) + cada int (4 bytes)
 *
 * NÃO usar writeUTF/readUTF em nenhuma entidade do projeto — incompatível
 * com o formato acima e com o esquema binário documentado.
 */
public class SerializadorUtil {

    private static final byte DELIMITADOR = (byte) ';';

    // -------------------------------------------------------------------------
    // String
    // -------------------------------------------------------------------------

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

    // -------------------------------------------------------------------------
    // ISBN — campo fixo de 13 bytes
    // -------------------------------------------------------------------------

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
            isbn[i] = (char) (bytes[i] & 0xFF);
        }
        return isbn;
    }

    // -------------------------------------------------------------------------
    // String[] — campo multivalorado (ex.: tags embutidas no registro)
    // -------------------------------------------------------------------------

    public static void writeStringArray(DataOutputStream dos, String[] valores) throws IOException {
        if (valores == null) {
            dos.writeInt(0);
            return;
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

    // -------------------------------------------------------------------------
    // int[]
    // -------------------------------------------------------------------------

    public static void writeIntArray(DataOutputStream dos, int[] valores) throws IOException {
        if (valores == null) {
            dos.writeInt(0);
            return;
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
