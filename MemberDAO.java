package library;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
// Using intrinsic synchronization instead of ReentrantReadWriteLock
import java.util.stream.Collectors;

/**
 * Data Access Object for Member entities with thread-safe operations.
 */
public class MemberDAO {
    private final Path path;
    // synchronized methods will provide thread-safety

    /**
     * Creates a new MemberDAO with default data path.
     */
    public MemberDAO() {
        this(Paths.get("data", "members.csv"));
    }

    /**
     * Creates a new MemberDAO with specified data path.
     * @param path the path to store member data
     * @throws NullPointerException if path is null
     */
    public MemberDAO(Path path) {
        this.path = Objects.requireNonNull(path, "Path cannot be null");
    }

    /**
     * Loads all members from storage.
     * @return unmodifiable list of all members, empty list if file doesn't exist or on error
     */
    public synchronized List<Member> loadAll() {
        try {
            if (!Files.exists(path)) {
                return Collections.emptyList();
            }
            List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
            return Collections.unmodifiableList(
                lines.stream()
                    .filter(line -> !line.trim().isEmpty())
                    .map(Member::fromCSV)
                    .collect(Collectors.toList())
            );
        } catch (IOException e) {
            System.err.println("Error loading members: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Saves all members to storage.
     * @param members the list of members to save
     * @throws IllegalArgumentException if members list is null
     * @throws UncheckedIOException if saving fails
     */
    public synchronized void saveAll(List<Member> members) {
        Objects.requireNonNull(members, "Members list cannot be null");
        try {
            StringBuilder sb = new StringBuilder();
            for (Member member : members) {
                if (member != null) {
                    sb.append(member.toCSV()).append(System.lineSeparator());
                }
            }
            FileUtil.writeAtomically(path, sb.toString());
        } catch (IOException e) {
            String msg = "Failed to save members: " + e.getMessage();
            System.err.println(msg);
            throw new UncheckedIOException(msg, e);
        }
    }

    /**
     * Finds a member by their ID.
     * @param id the ID to search for
     * @return the member if found, null otherwise
     * @throws IllegalArgumentException if id is null
     */
    public Member findById(String id) {
        Objects.requireNonNull(id, "Member ID cannot be null");
        return loadAll().stream()
                .filter(m -> id.equals(m.getId()))
                .findFirst()
                .orElse(null);
    }

    /**
     * Adds a new member to storage.
     * @param member the member to add
     * @throws IllegalArgumentException if member is null or if member with same ID exists
     * @throws UncheckedIOException if saving fails
     */
    public synchronized void add(Member member) {
        Objects.requireNonNull(member, "Member cannot be null");
        List<Member> all = new ArrayList<>(loadAll());
        // Check for duplicate ID
        if (all.stream().anyMatch(m -> member.getId().equals(m.getId()))) {
            throw new IllegalArgumentException("Member with ID " + member.getId() + " already exists");
        }
        all.add(member);
        saveAll(all);
    }

    /**
     * Updates an existing member or adds them if not found.
     * @param member the member to update
     * @throws IllegalArgumentException if member is null
     * @throws UncheckedIOException if saving fails
     */
    public synchronized void update(Member member) {
        Objects.requireNonNull(member, "Member cannot be null");
        List<Member> all = new ArrayList<>(loadAll());
        boolean found = false;

        for (int i = 0; i < all.size(); i++) {
            if (member.getId().equals(all.get(i).getId())) {
                all.set(i, member);
                found = true;
                break;
            }
        }

        if (!found) {
            all.add(member);
        }

        saveAll(all);
    }
}
