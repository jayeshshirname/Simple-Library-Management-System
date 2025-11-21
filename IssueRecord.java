package library;

import java.time.LocalDate;
import java.util.List;
import library.util.CsvUtil;

public class IssueRecord {
    private String id;
    private String bookId;
    private String memberId;
    private LocalDate issueDate;
    private LocalDate dueDate;
    private LocalDate returnDate; // null if not returned
    private double fine;
    private String action; // ISSUE or RETURN

    public IssueRecord() {}

    public IssueRecord(String id, String bookId, String memberId, LocalDate issueDate, LocalDate dueDate, LocalDate returnDate, double fine, String action) {
        this.id = id; this.bookId = bookId; this.memberId = memberId; this.issueDate = issueDate; this.dueDate = dueDate; this.returnDate = returnDate; this.fine = fine; this.action = action;
    }

    // getters & setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getBookId() { return bookId; }
    public void setBookId(String bookId) { this.bookId = bookId; }
    public String getMemberId() { return memberId; }
    public void setMemberId(String memberId) { this.memberId = memberId; }
    public LocalDate getIssueDate() { return issueDate; }
    public void setIssueDate(LocalDate issueDate) { this.issueDate = issueDate; }
    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }
    public LocalDate getReturnDate() { return returnDate; }
    public void setReturnDate(LocalDate returnDate) { this.returnDate = returnDate; }
    public double getFine() { return fine; }
    public void setFine(double fine) { this.fine = fine; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String toCSV() {
        return CsvUtil.join(
                id,
                bookId,
                memberId,
                issueDate == null ? "" : issueDate.toString(),
                dueDate == null ? "" : dueDate.toString(),
                returnDate == null ? "" : returnDate.toString(),
                String.valueOf(fine),
                action);
    }

    public static IssueRecord fromCSV(String line) {
        List<String> fields = CsvUtil.parseLine(line);
        IssueRecord r = new IssueRecord();
        r.id = !fields.isEmpty() ? CsvUtil.unescape(fields.get(0)) : null;
        r.bookId = fields.size() > 1 ? CsvUtil.unescape(fields.get(1)) : null;
        r.memberId = fields.size() > 2 ? CsvUtil.unescape(fields.get(2)) : null;
        r.issueDate = fields.size() > 3 && !fields.get(3).isEmpty() ? LocalDate.parse(CsvUtil.unescape(fields.get(3))) : null;
        r.dueDate = fields.size() > 4 && !fields.get(4).isEmpty() ? LocalDate.parse(CsvUtil.unescape(fields.get(4))) : null;
        r.returnDate = fields.size() > 5 && !fields.get(5).isEmpty() ? LocalDate.parse(CsvUtil.unescape(fields.get(5))) : null;
        r.fine = fields.size() > 6 && !fields.get(6).isEmpty() ? Double.parseDouble(CsvUtil.unescape(fields.get(6))) : 0.0;
        r.action = fields.size() > 7 ? CsvUtil.unescape(fields.get(7)) : null;
        return r;
    }

    @Override
    public String toString() {
        return String.format("%s | %s | %s | %s -> %s | %s | fine=%.2f", id, bookId, memberId, issueDate, dueDate, action, fine);
    }
}
