package library;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.*;
// Using intrinsic synchronization instead of ReentrantReadWriteLock
import java.util.stream.Collectors;

/**
 * Data Access Object for Transaction records with thread-safe operations.
 * Stores transactions in date-based CSV files.
 */
public class TransactionDAO {
    // synchronized methods will provide thread-safety
    private static final String HEADER = "txId,bookId,memberId,issueDate,dueDate,returnDate,fine,action";
    private final Path dataDir;

    /**
     * Creates a new TransactionDAO with default data directory.
     */
    public TransactionDAO() {
        this(Paths.get("data"));
    }

    /**
     * Creates a new TransactionDAO with specified data directory.
     * @param dataDir the directory to store transaction files
     */
    public TransactionDAO(Path dataDir) {
        this.dataDir = Objects.requireNonNull(dataDir, "Data directory path cannot be null");
    }

    /**
     * Gets the path for storing transactions for a specific date.
     * @param date the date to get path for
     * @return the path for the transaction file
     */
    private Path getPathForDate(LocalDate date) {
        Objects.requireNonNull(date, "Date cannot be null");
        return dataDir.resolve("transactions_" + date.toString() + ".csv");
    }

    /**
     * Appends a transaction record for a specific date.
     * @param record the transaction record to append
     * @param date the date of the transaction
     * @throws IllegalArgumentException if record or date is null
     * @throws UncheckedIOException if IO operation fails
     */
    public void append(IssueRecord record, LocalDate date) {
        Objects.requireNonNull(record, "Transaction record cannot be null");
        Objects.requireNonNull(date, "Date cannot be null");

        synchronized (this) {
            try {
                Path path = getPathForDate(date);
                boolean exists = Files.exists(path);

                if (!exists) {
                    Files.createDirectories(path.getParent());
                    Files.write(path,
                        (HEADER + System.lineSeparator()).getBytes(StandardCharsets.UTF_8),
                        StandardOpenOption.CREATE);
                }

                FileUtil.appendLineWithLock(path, record.toCSV());
            } catch (IOException e) {
                String msg = String.format("Failed to append transaction on date %s: %s", date, e.getMessage());
                System.err.println(msg);
                throw new UncheckedIOException(msg, e);
            }
        }
    }

    /**
     * Loads all transaction records for a specific date.
     * @param date the date to load transactions for
     * @return unmodifiable list of transaction records, empty list if none found
     * @throws IllegalArgumentException if date is null
     */
    public List<IssueRecord> loadForDate(LocalDate date) {
        Objects.requireNonNull(date, "Date cannot be null");
        
        synchronized (this) {
            try {
                Path path = getPathForDate(date);
                if (!Files.exists(path)) {
                    return Collections.emptyList();
                }

                List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
                if (lines.size() <= 1) {
                    return Collections.emptyList();
                }

                return Collections.unmodifiableList(
                    lines.stream()
                        .skip(1) // Skip header
                        .filter(line -> !line.trim().isEmpty())
                        .map(IssueRecord::fromCSV)
                        .collect(Collectors.toList())
                );
            } catch (IOException e) {
                String msg = String.format("Failed to load transactions for date %s: %s", date, e.getMessage());
                System.err.println(msg);
                return Collections.emptyList();
            }
        }
    }
}
