package library;

import java.util.List;

/**
 * Compatibility wrapper for CSV utilities. Delegates to {@code library.util.CsvUtil}.
 */
public final class CsvUtil {
    private CsvUtil() {}

    public static String escape(String field) { return library.util.CsvUtil.escape(field); }
    public static String unescape(String field) { return library.util.CsvUtil.unescape(field); }
    public static List<String> parseLine(String line) { return library.util.CsvUtil.parseLine(line); }
    public static String join(List<String> fields) { return library.util.CsvUtil.join(fields); }
    public static String join(String... fields) { return library.util.CsvUtil.join(fields); }
}