package library;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

/**
 * Data Access Object for Book entities with thread-safe operations.
 */
public class BookDAO {
    private final Path path;
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    public BookDAO() {
        this(Paths.get("data", "books.csv"));
    }

    public BookDAO(Path path) {
        this.path = Objects.requireNonNull(path, "Path cannot be null");
    }

    /**
     * Loads all books from storage.
     * @return Unmodifiable list of all books, empty list if file doesn't exist or on error
     */
    public List<Book> loadAll() {
        lock.readLock().lock();
        try {
            if (!Files.exists(path)) {
                return Collections.emptyList();
            }
            List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
            return Collections.unmodifiableList(
                lines.stream()
                    .filter(line -> !line.trim().isEmpty())
                    .map(Book::fromCSV)
                    .collect(Collectors.toList())
            );
        } catch (IOException e) {
            System.err.println("Error loading books: " + e.getMessage());
            return Collections.emptyList();
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Saves all books to storage.
     * @param books List of books to save
     * @throws IllegalArgumentException if books list is null
     */
    public void saveAll(List<Book> books) {
        Objects.requireNonNull(books, "Books list cannot be null");
        
        lock.writeLock().lock();
        try {
            StringBuilder sb = new StringBuilder();
            for (Book b : books) {
                if (b != null) {
                    sb.append(b.toCSV()).append(System.lineSeparator());
                }
            }
            FileUtil.writeAtomically(path, sb.toString());
        } catch (IOException e) {
            System.err.println("Error saving books: " + e.getMessage());
            throw new UncheckedIOException("Failed to save books", e);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Finds a book by its ID.
     * @param id The ID to search for
     * @return The book if found, null otherwise
     * @throws IllegalArgumentException if id is null
     */
    public Book findById(String id) {
        Objects.requireNonNull(id, "Book ID cannot be null");
        return loadAll().stream()
                .filter(b -> id.equals(b.getId()))
                .findFirst()
                .orElse(null);
    }

    /**
     * Adds a new book to storage.
     * @param book The book to add
     * @throws IllegalArgumentException if book is null
     * @throws UncheckedIOException if saving fails
     */
    public void add(Book book) {
        Objects.requireNonNull(book, "Book cannot be null");
        
        lock.writeLock().lock();
        try {
            List<Book> all = new ArrayList<>(loadAll());
            // Check for duplicate ID
            if (all.stream().anyMatch(b -> book.getId().equals(b.getId()))) {
                throw new IllegalArgumentException("Book with ID " + book.getId() + " already exists");
            }
            all.add(book);
            saveAll(all);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Updates an existing book or adds it if not found.
     * @param book The book to update
     * @throws IllegalArgumentException if book is null
     * @throws UncheckedIOException if saving fails
     */
    public void update(Book book) {
        Objects.requireNonNull(book, "Book cannot be null");
        
        lock.writeLock().lock();
        try {
            List<Book> all = new ArrayList<>(loadAll());
            boolean found = false;
            
            for (int i = 0; i < all.size(); i++) {
                if (book.getId().equals(all.get(i).getId())) {
                    all.set(i, book);
                    found = true;
                    break;
                }
            }
            
            if (!found) {
                all.add(book);
            }
            
            saveAll(all);
        } finally {
            lock.writeLock().unlock();
        }
    }
}
