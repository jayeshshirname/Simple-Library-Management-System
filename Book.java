package library;

import java.util.List;
import library.util.CsvUtil;

public class Book {
    private String id;
    private String title;
    private String author;
    private String category;
    private int totalCopies;
    private int availableCopies;

    public Book() {}

    public Book(String id, String title, String author, String category, int totalCopies, int availableCopies) {
        this.id = id; this.title = title; this.author = author; this.category = category;
        this.totalCopies = totalCopies; this.availableCopies = availableCopies;
    }

    // getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public int getTotalCopies() { return totalCopies; }
    public void setTotalCopies(int totalCopies) { this.totalCopies = totalCopies; }
    public int getAvailableCopies() { return availableCopies; }
    public void setAvailableCopies(int availableCopies) { this.availableCopies = availableCopies; }

    public String toCSV() {
        return CsvUtil.join(id, title, author, category, String.valueOf(totalCopies), String.valueOf(availableCopies));
    }

    public static Book fromCSV(String line) {
        List<String> fields = CsvUtil.parseLine(line);
        String id = !fields.isEmpty() ? CsvUtil.unescape(fields.get(0)) : null;
        String title = fields.size() > 1 ? CsvUtil.unescape(fields.get(1)) : null;
        String author = fields.size() > 2 ? CsvUtil.unescape(fields.get(2)) : null;
        String category = fields.size() > 3 ? CsvUtil.unescape(fields.get(3)) : null;
        int total = fields.size() > 4 && !fields.get(4).isEmpty() ? Integer.parseInt(CsvUtil.unescape(fields.get(4))) : 0;
        int avail = fields.size() > 5 && !fields.get(5).isEmpty() ? Integer.parseInt(CsvUtil.unescape(fields.get(5))) : 0;
        return new Book(id, title, author, category, total, avail);
    }

    @Override
    public String toString() {
        return String.format("%s | %s | %s | %d/%d", id, title, author, availableCopies, totalCopies);
    }
}