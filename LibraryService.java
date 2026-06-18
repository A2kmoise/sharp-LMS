import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class LibraryService {
    private final DatabaseConnection db;

    public LibraryService(DatabaseConnection db) {
        this.db = db;
    }

    public synchronized boolean borrowBook(Member member, String isbn) {
        try (Connection conn = db.getConnection()) {

            try (PreparedStatement checkMemberExists = conn.prepareStatement(
                    "SELECT 1 FROM members WHERE member_id = ?")) {
                checkMemberExists.setInt(1, member.getId());
                ResultSet rs = checkMemberExists.executeQuery();
                if (!rs.next()) {
                    System.out.printf("\n >Validation Failed: Member ID %d is not registered in the database.\n", member.getId());
                    return false;
                }
            }

            try (PreparedStatement checkMember = conn.prepareStatement(
                    "SELECT COUNT(*) FROM borrowing_records WHERE member_id = ? AND return_date IS NULL")) {
                checkMember.setInt(1, member.getId());
                ResultSet rs = checkMember.executeQuery();
                if (rs.next() && rs.getInt(1) >= 5) {
                    System.out.printf("\n > Limit Reached: Member '%s' already has 5 books borrowed.\n", member.getName());
                    return false;
                }
            }

            Book book;
            try (PreparedStatement checkBook = conn.prepareStatement(
                    "SELECT title, author, publication_year FROM books WHERE isbn = ? AND is_available = TRUE")) {
                checkBook.setString(1, isbn);
                ResultSet rs = checkBook.executeQuery();
                if (!rs.next()) {
                    System.out.printf("\n > Unavailable: Book '%s' is not available for borrowing.\n", isbn);
                    return false;
                }
                book = new Book(isbn, rs.getString("title"), rs.getString("author"), rs.getInt("publication_year"), false);
            }

            try (PreparedStatement updateBook = conn.prepareStatement("UPDATE books SET is_available = FALSE WHERE isbn = ?")) {
                updateBook.setString(1, isbn);
                updateBook.executeUpdate();
            }

            try (PreparedStatement insertRecord = conn.prepareStatement(
                    "INSERT INTO borrowing_records (member_id, isbn, borrow_date) VALUES (?, ?, ?)")) {
                insertRecord.setInt(1, member.getId());
                insertRecord.setString(2, isbn);
                insertRecord.setDate(3, java.sql.Date.valueOf(LocalDate.now()));
                insertRecord.executeUpdate();
            }

            member.addBorrowedBook(book);
            System.out.printf("Success: Book '%s' borrowed by '%s'.\n", isbn, member.getName());
            return true;

        } catch (SQLException e) {
            System.out.printf("Database Error (Borrow): %s\n", e.getMessage());
            return false;
        }
    }

    public synchronized boolean returnBook(Member member, String isbn) {
        try (Connection conn = db.getConnection()) {

            try (PreparedStatement updateRecord = conn.prepareStatement(
                    "UPDATE borrowing_records SET return_date = ? WHERE member_id = ? AND isbn = ? AND return_date IS NULL")) {
                updateRecord.setDate(1, java.sql.Date.valueOf(LocalDate.now()));
                updateRecord.setInt(2, member.getId());
                updateRecord.setString(3, isbn);
                if (updateRecord.executeUpdate() == 0) {
                    System.out.printf("Not Found: No active borrow record for book '%s' by member ID %d.\n", isbn, member.getId());
                    return false;
                }
            }

            try (PreparedStatement updateBook = conn.prepareStatement("UPDATE books SET is_available = TRUE WHERE isbn = ?")) {
                updateBook.setString(1, isbn);
                updateBook.executeUpdate();
            }

            member.removeBorrowedBook(isbn);
            System.out.printf("Success: Book '%s' returned by '%s'.\n", isbn, member.getName());
            return true;

        } catch (SQLException e) {
            System.out.printf("Database Error (Return): %s\n", e.getMessage());
            return false;
        }
    }

    public List<Book> listAvailableBooks() {
        List<Book> availableBooks = new ArrayList<>();
        try (Connection conn = db.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT isbn, title, author, publication_year FROM books WHERE is_available = TRUE");
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                availableBooks.add(new Book(
                    rs.getString("isbn"),
                    rs.getString("title"),
                    rs.getString("author"),
                    rs.getInt("publication_year"),
                    true
                ));
            }
        } catch (SQLException e) {
            System.out.printf("Database Error (Listing): %s\n", e.getMessage());
        }
        return availableBooks;
    }

    public List<Book> listAllBooks() {
        List<Book> allBooks = new ArrayList<>();
        try (Connection conn = db.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT isbn, title, author, publication_year, is_available FROM books")) {
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                allBooks.add(new Book(
                        rs.getString("isbn"),
                        rs.getString("title"),
                        rs.getString("author"),
                        rs.getInt("publication_year"),
                        rs.getBoolean("is_available")));
            }
        } catch (SQLException e) {
            System.out.printf("Database Error (List All): %s\n", e.getMessage());
        }
        return allBooks;
    }
    public boolean deleteBook(String isbn) {
        try (Connection conn = db.getConnection()) {
            try (PreparedStatement deleteRecords = conn.prepareStatement(
                    "DELETE FROM borrowing_records WHERE isbn = ?")) {
                deleteRecords.setString(1, isbn);
                deleteRecords.executeUpdate();
            }
            try (PreparedStatement deleteBook = conn.prepareStatement(
                    "DELETE FROM books WHERE isbn = ?")) {
                deleteBook.setString(1, isbn);
                int affected = deleteBook.executeUpdate();
                if (affected == 0) {
                    System.out.printf("No book with ISBN '%s' found.\n", isbn);
                    return false;
                }
                System.out.printf("Book with ISBN '%s' deleted successfully.\n", isbn);
                return true;
            }
        } catch (SQLException e) {
            System.out.printf("Database Error (Delete): %s\n", e.getMessage());
            return false;
        }
    }

    public boolean updateBook(String isbn, String newTitle, String newAuthor, int newYear) {
        try (Connection conn = db.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "UPDATE books SET title = ?, author = ?, publication_year = ? WHERE isbn = ?")) {
            stmt.setString(1, newTitle);
            stmt.setString(2, newAuthor);
            stmt.setInt(3, newYear);
            stmt.setString(4, isbn);
            int affected = stmt.executeUpdate();
            if (affected == 0) {
                System.out.printf("No book with ISBN '%s' found.\n", isbn);
                return false;
            }
            System.out.printf("Book with ISBN '%s' updated successfully.\n", isbn);
            return true;
        } catch (SQLException e) {
            System.out.printf("Database Error (Update): %s\n", e.getMessage());
            return false;
        }
    }
}
