package src.util;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Utilitário de conversão de datas.
 * Armazena as datas como timestamp Unix em milissegundos (long).
 * Utiliza a API moderna java.time (imutável e thread-safe).
 */
public final class DataUtil {

    private static final DateTimeFormatter FORMATO_BR = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // Construtor privado para evitar instanciação de classe utilitária
    private DataUtil() {}

    /**
     * Converte uma string no formato "dd/MM/yyyy" para timestamp (long).
     */
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

    /**
     * Converte um timestamp (long) de volta para uma String no formato "dd/MM/yyyy".
     */
    public static String timestampToString(long timestamp) {
        return Instant.ofEpochMilli(timestamp)
                      .atZone(ZoneId.systemDefault())
                      .toLocalDate()
                      .format(FORMATO_BR);
    }
}