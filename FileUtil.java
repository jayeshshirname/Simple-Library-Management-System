package library;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Compatibility wrapper for file utilities. Delegates to {@code library.util.FileUtil}.
 *
 * This wrapper exists to avoid changing many files at once — it forwards calls
 * to the real implementation in package library.util.
 */
public final class FileUtil {
    private FileUtil() {}

    public static void appendLineWithLock(Path path, String line) throws IOException {
        library.util.FileUtil.appendLineWithLock(path, line);
    }

    public static void writeAtomically(Path path, String content) throws IOException {
        library.util.FileUtil.writeAtomically(path, content);
    }

    public static void ensureDataDir() throws IOException {
        library.util.FileUtil.ensureDataDir();
    }
}

