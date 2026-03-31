package src.util;

import java.text.SimpleDateFormat;
import java.util.Date;

public class DataUtil {
    private static final SimpleDateFormat formatoBR = new SimpleDateFormat("dd/MM/yyyy");

    // Converte uma string de data no formato dd/mm/aaaa para timestamp (long)
    public static long stringToTimestamp(String data) throws Exception {
        try {
            Date date = formatoBR.parse(data);
            return date.getTime();
        } catch (Exception e) {
            throw new Exception("Formato de data inválido. Use dd/mm/aaaa");
        }
    }

    // Converte um timestamp (long) para string no formato dd/mm/aaaa
    public static String timestampToString(long timestamp) {
        Date date = new Date(timestamp);
        return formatoBR.format(date);
    }
}
