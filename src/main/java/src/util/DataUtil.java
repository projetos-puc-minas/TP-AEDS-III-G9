package src.util;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;


public final class DataUtil {

    private static final DateTimeFormatter FORMATO_BR = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // Construtor privado para evitar instanciação de classe utilitária
    private DataUtil() {}

    public static long stringToTimestamp(String data) throws IllegalArgumentException {
        try {
            LocalDate date = LocalDate.parse(data, FORMATO_BR);
            return date.atStartOfDay(ZoneId.systemDefault())
                       .toInstant()
                       .toEpochMilli();
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Formato de data inválido. Use dd/MM/yyyy");
        }
    }


    public static String timestampToString(long timestamp) {
        return Instant.ofEpochMilli(timestamp)
                      .atZone(ZoneId.systemDefault())
                      .toLocalDate()
                      .format(FORMATO_BR);
    }
}