package library;

import java.time.LocalDate;
import java.util.*;
import library.dao.BookDAO;
import library.dao.MemberDAO;
import library.dao.TransactionDAO;
import library.model.Book;
import library.model.IssueRecord;
import library.model.Member;

/**
 * Main service class for the library management system.
 * Handles books, members, and transactions with thread-safety.
 */
public final class LibraryService {
    private final BookDAO bookDao;
    private final MemberDAO memberDao;
    private final TransactionDAO txDao;
    private static final int DEFAULT_LOAN_DAYS = 14;
    private static final double FINE_PER_DAY = 5.0;

    public LibraryService() {
        this.bookDao = new BookDAO();
        this.memberDao = new MemberDAO();
        this.txDao = new TransactionDAO();
    }

    /**
     * Adds a new book to the library.
     * @param book the book to add
     * @throws NullPointerException if book is null
     */
    public synchronized void addBook(Book book) {
        Objects.requireNonNull(book, "Book cannot be null");
        bookDao.add(book);
    }

    /**
     * Lists all books in the library.
     * @return unmodifiable list of all books
     */
    public synchronized List<Book> listBooks() {
        return Collections.unmodifiableList(bookDao.loadAll());
    }

    /**
     * Finds a book by its ID.
     * @param id the book ID
     * @return the book or null if not found
     */
    public synchronized Book findBook(String id) {
        if (id == null || id.trim().isEmpty()) {
            return null;
        }
        return bookDao.findById(id);
    }

    /**
     * Adds a new member to the library.
     * @param member the member to add
     * @throws NullPointerException if member is null
     */
    public synchronized void addMember(Member member) {
        Objects.requireNonNull(member, "Member cannot be null");
        memberDao.add(member);
    }

    /**
     * Finds a member by their ID.
     * @param id the member ID
     * @return the member or null if not found
     */
    public synchronized Member findMember(String id) {
        if (id == null || id.trim().isEmpty()) {
            return null;
        }
        return memberDao.findById(id);
    }

    /**
     * Authenticates a member with ID and password.
     * @param id member ID
     * @param password member password
     * @return Optional containing the member if authentication successful
     */
    public synchronized Optional<Member> authenticate(String id, String password) {
        if (id == null || password == null) {
            return Optional.empty();
        }
        
        Member member = memberDao.findById(id);
        if (member != null && member.checkPassword(password)) {
            memberDao.update(member);
            return Optional.of(member);
        }
        return Optional.empty();
    }

    /**
     * Issues a book to a member.
     * @param bookId the book ID
     * @param memberId the member ID
     * @return the issue record
     * @throws IllegalArgumentException if book or member not found or no copies available
     */
    public synchronized IssueRecord issueBook(String bookId, String memberId) throws IllegalArgumentException {
        Objects.requireNonNull(bookId, "Book ID cannot be null");
        Objects.requireNonNull(memberId, "Member ID cannot be null");

        Book book = bookDao.findById(bookId);
        if (book == null) {
            throw new IllegalArgumentException("Book not found: " + bookId);
        }
        if (book.getAvailableCopies() <= 0) {
            throw new IllegalArgumentException("No copies available for book: " + bookId);
        }

        Member member = memberDao.findById(memberId);
        if (member == null) {
            throw new IllegalArgumentException("Member not found: " + memberId);
        }

        book.setAvailableCopies(book.getAvailableCopies() - 1);
        bookDao.update(book);

        LocalDate issueDate = LocalDate.now();
        IssueRecord record = new IssueRecord(
            UUID.randomUUID().toString(),
            bookId,
            memberId,
            issueDate,
            issueDate.plusDays(DEFAULT_LOAN_DAYS),
            null,
            0.0,
            "ISSUE"
        );

        txDao.append(record, issueDate);
        return record;
    }

    /**
     * Returns a book and calculates any fines.
     * @param txId the transaction ID
     * @param bookId the book ID
     * @param memberId the member ID
     * @return the return record
     * @throws IllegalArgumentException if book, member, or transaction not found
     */
    public synchronized IssueRecord returnBook(String txId, String bookId, String memberId) throws IllegalArgumentException {
        Objects.requireNonNull(txId, "Transaction ID cannot be null");
        Objects.requireNonNull(bookId, "Book ID cannot be null");
        Objects.requireNonNull(memberId, "Member ID cannot be null");

        Book book = bookDao.findById(bookId);
        if (book == null) {
            throw new IllegalArgumentException("Book not found: " + bookId);
        }

        Member member = memberDao.findById(memberId);
        if (member == null) {
            throw new IllegalArgumentException("Member not found: " + memberId);
        }

        LocalDate returnDate = LocalDate.now();
        List<IssueRecord> transactions = txDao.loadForDate(returnDate);
        IssueRecord original = transactions.stream()
            .filter(r -> r.getId().equals(txId) && "ISSUE".equalsIgnoreCase(r.getAction()))
            .findFirst()
            .orElse(null);

        LocalDate dueDate;
        if (original != null) {
            dueDate = original.getDueDate();
        } else if (!transactions.isEmpty()) {
            dueDate = transactions.get(0).getDueDate();
        } else {
            dueDate = returnDate; // No fine if original record not found
        }

        long daysLate = returnDate.toEpochDay() - dueDate.toEpochDay();
        double fine = Math.max(0, daysLate * FINE_PER_DAY);

        book.setAvailableCopies(book.getAvailableCopies() + 1);
        bookDao.update(book);

        IssueRecord returnRecord = new IssueRecord(
            txId,
            bookId,
            memberId,
            null,
            dueDate,
            returnDate,
            fine,
            "RETURN"
        );

        txDao.append(returnRecord, returnDate);
        return returnRecord;
    }

    /**
     * Counts the number of issues on a specific date.
     * @param date the date to count issues for
     * @return the number of issues
     */
    public synchronized long countIssuesOn(LocalDate date) {
        Objects.requireNonNull(date, "Date cannot be null");
        return txDao.loadForDate(date).stream()
            .filter(r -> "ISSUE".equalsIgnoreCase(r.getAction()))
            .count();
    }

    /**
     * Lists all transactions for a specific date.
     * @param date the date to list transactions for
     * @return unmodifiable list of transactions
     */
    public synchronized List<IssueRecord> listTransactions(LocalDate date) {
        Objects.requireNonNull(date, "Date cannot be null");
        return Collections.unmodifiableList(txDao.loadForDate(date));
    }
}
