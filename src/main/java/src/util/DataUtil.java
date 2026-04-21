package src.util;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Utilitário de conversão de datas.
 *
 * Usa java.time (imutável e thread-safe) em vez de SimpleDateFormat.
 * Armazenamento: timestamp Unix em milissegundos (long), compatível com
 * o campo dataNascimento de Autores.
 */
public class DataUtil {

    // DateTimeFormatter é imutável e thread-safe
    private static final DateTimeFormatter FORMATO_BR =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");

    /**
     * Converte "dd/MM/yyyy" → timestamp Unix em milissegundos.
     */
    public static long stringToTimestamp(String data) throws Exception {
        try {
            LocalDate date = LocalDate.parse(data, FORMATO_BR);
            return date.atStartOfDay(ZoneId.systemDefault())
                       .toInstant()
                       .toEpochMilli();
        } catch (DateTimeParseException e) {
            throw new Exception("Formato de data inválido. Use dd/MM/yyyy");
        }
    }

    /**
     * Converte timestamp Unix em milissegundos → "dd/MM/yyyy".
     */
    public static String timestampToString(long timestamp) {
        return Instant.ofEpochMilli(timestamp)
                      .atZone(ZoneId.systemDefault())
                      .toLocalDate()
                      .format(FORMATO_BR);
    }
}
