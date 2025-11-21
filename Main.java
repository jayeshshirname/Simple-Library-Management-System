package library;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.List;
import java.util.Scanner;
import java.util.UUID;

import library.model.Book;
import library.model.Member;
import library.model.IssueRecord;
import library.dao.MemberDAO;
import library.dao.TransactionDAO;
import library.servicelibrary.service.LibraryService;

/**
 * Main class for the Library Management System CLI application.
 * Provides a command-line interface for managing books, members, and transactions.
 */
public final class Main {
    // Constants
    private static final Path DATA_DIR = Paths.get("data");
    
    
    
    
    // Static resources
    private static final LibraryService SERVICE = new LibraryService();
    private static final Scanner SCANNER = new Scanner(System.in);

    /**
     * Private constructor to prevent instantiation.
     */
    private Main() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Sets up the data directory for storing files.
     * @throws IOException if directory creation fails
     */
    private static void setupDataDirectory() throws IOException {
        if (!Files.exists(DATA_DIR)) {
            Files.createDirectories(DATA_DIR);
        }
    }

    /**
     * Main entry point for the library management system.
     * @param args command line arguments (not used)
     */
    public static void main(String[] args) {
        System.out.println("Jayesh Simple Library management System.");
        
        try {
            setupDataDirectory();
            seed();

            while (true) {
                try {
                    displayMenu();
                    String choice = SCANNER.nextLine().trim();
                    
                    switch (choice) {
                        case "1" -> addBookFlow();
                        case "2" -> listBooks();
                        case "3" -> addMemberFlow();
                        case "4" -> issueFlow();
                        case "5" -> returnFlow();
                        case "6" -> todaysReport();
                        case "0" -> {
                            System.out.println("Goodbye!");
                            closeResources();
                            System.exit(0);
                        }
                        default -> System.out.println("Invalid option, please try again.");
                    }
                } catch (IllegalArgumentException | SecurityException e) {
                    System.err.println("Error: " + e.getMessage());
                }
            }
        } catch (Exception e) {
            System.err.println("Fatal error: " + e.getMessage());
            closeResources();
            System.exit(1);
        }
    }
    
    /**
     * Displays the main menu options.
     */
    private static void displayMenu() {
        System.out.println("\nMenu:");
        System.out.println("1. Add Book");
        System.out.println("2. List Books");
        System.out.println("3. Add Member");
        System.out.println("4. Issue Book");
        System.out.println("5. Return Book");
        System.out.println("6. Today's Issues (count + list)");
        System.out.println("0. Exit");
        System.out.print("Choose: ");
    }
    /**
     * Closes all resources when exiting the application.
     */
    private static void closeResources() {
        if (SCANNER != null) {
            SCANNER.close();
        }
    }

    /**
     * Seeds the database with initial admin user if none exists.
     */
    private static void seed() {
        MemberDAO mdao = new MemberDAO();
        if (mdao.loadAll().isEmpty()) {
            try {
                Member admin = new Member(
                    "admin",
                    "System Administrator",
                    "admin@library.local",
                    "",
                    "admin123",
                    true
                );
                mdao.add(admin);
                System.out.println("Created default admin user (ID: admin, Password: admin123)");
            } catch (Exception e) {
                System.err.println("Failed to create admin user: " + e.getMessage());
            }
        }
    }

    /**
     * Handles the book addition flow.
     */
    private static void addBookFlow() {
        try {
            System.out.print("Title: ");
            String title = SCANNER.nextLine().trim();
            if (title.isEmpty()) {
                throw new IllegalArgumentException("Title cannot be empty");
            }

            System.out.print("Author: ");
            String author = SCANNER.nextLine().trim();
            if (author.isEmpty()) {
                throw new IllegalArgumentException("Author cannot be empty");
            }

            System.out.print("Category: ");
            String category = SCANNER.nextLine().trim();
            if (category.isEmpty()) {
                throw new IllegalArgumentException("Category cannot be empty");
            }

            System.out.print("Total copies: ");
            int totalCopies = Integer.parseInt(SCANNER.nextLine().trim());
            if (totalCopies <= 0) {
                throw new IllegalArgumentException("Total copies must be positive");
            }

            Book book = new Book(
                UUID.randomUUID().toString(),
                title,
                author,
                category,
                totalCopies,
                totalCopies
            );
            SERVICE.addBook(book);
            System.out.println("Added: " + book);
        } catch (NumberFormatException e) {
            System.err.println("Error: Total copies must be a valid number");
        }
    }

    /**
     * Displays all books in the library.
     */
    private static void listBooks() {
        List<Book> books = SERVICE.listBooks();
        if (books.isEmpty()) {
            System.out.println("No books in the library.");
            return;
        }
        
        System.out.println("\nLibrary Books:");
        books.forEach(book -> System.out.println(book.toString()));
    }

    /**
     * Handles the member addition flow.
     */
    private static void addMemberFlow() {
        try {
            System.out.print("Member ID: ");
            String id = SCANNER.nextLine().trim();
            if (id.isEmpty()) {
                throw new IllegalArgumentException("Member ID cannot be empty");
            }

            System.out.print("Name: ");
            String name = SCANNER.nextLine().trim();
            if (name.isEmpty()) {
                throw new IllegalArgumentException("Name cannot be empty");
            }

            System.out.print("Email: ");
            String email = SCANNER.nextLine().trim();
            if (email.isEmpty()) {
                throw new IllegalArgumentException("Email cannot be empty");
            }

            System.out.print("Phone: ");
            String phone = SCANNER.nextLine().trim();

            System.out.print("Password: ");
            String password = SCANNER.nextLine().trim();
            if (password.length() < 8) {
                throw new IllegalArgumentException("Password must be at least 8 characters");
            }

            System.out.print("Is admin (true/false): ");
            boolean isAdmin = Boolean.parseBoolean(SCANNER.nextLine().trim());

            Member member = new Member(id, name, email, phone, password, isAdmin);
            SERVICE.addMember(member);
            System.out.println("Member added successfully.");
        } catch (IllegalArgumentException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }

    /**
     * Handles the book issue flow.
     */
    private static void issueFlow() {
        try {
            System.out.print("Book ID: ");
            String bookId = SCANNER.nextLine().trim();
            if (bookId.isEmpty()) {
                throw new IllegalArgumentException("Book ID cannot be empty");
            }

            System.out.print("Member ID: ");
            String memberId = SCANNER.nextLine().trim();
            if (memberId.isEmpty()) {
                throw new IllegalArgumentException("Member ID cannot be empty");
            }

            IssueRecord record = SERVICE.issueBook(bookId, memberId);
            System.out.println("Book issued successfully: " + record);
        } catch (IllegalArgumentException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }

    /**
     * Handles the book return flow.
     */
    private static void returnFlow() {
        try {
            System.out.print("Transaction ID: ");
            String txId = SCANNER.nextLine().trim();
            if (txId.isEmpty()) {
                throw new IllegalArgumentException("Transaction ID cannot be empty");
            }

            System.out.print("Book ID: ");
            String bookId = SCANNER.nextLine().trim();
            if (bookId.isEmpty()) {
                throw new IllegalArgumentException("Book ID cannot be empty");
            }

            System.out.print("Member ID: ");
            String memberId = SCANNER.nextLine().trim();
            if (memberId.isEmpty()) {
                throw new IllegalArgumentException("Member ID cannot be empty");
            }

            IssueRecord record = SERVICE.returnBook(txId, bookId, memberId);
            System.out.printf("Book returned successfully. Fine: $%.2f%n", record.getFine());
        } catch (IllegalArgumentException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }

    /**
     * Displays today's transaction report.
     */
    private static void todaysReport() {
        LocalDate today = LocalDate.now();
        long count = SERVICE.countIssuesOn(today);
        System.out.println("\nToday's Transactions Report:");
        System.out.println("Books issued today: " + count);
        
        List<IssueRecord> transactions = SERVICE.listTransactions(today);
        if (transactions.isEmpty()) {
            System.out.println("No transactions recorded today.");
        } else {
            System.out.println("\nTransaction details:");
            transactions.forEach(tx -> System.out.println(tx.toString()));
        }
    }
}
