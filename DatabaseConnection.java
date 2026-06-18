import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseConnection {
    private final String dbUrl;
    private final String user;
    private final String password;

    public DatabaseConnection(String dbUrl, String user, String password) {
        this.dbUrl = dbUrl;
        this.user = user;
        this.password = password;
    }

    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(dbUrl, user, password);
    }

    public void initializeDatabase() {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {


            //creating tables

            stmt.execute("CREATE TABLE IF NOT EXISTS books (" +
                    "isbn VARCHAR(20) PRIMARY KEY, " +
                    "title VARCHAR(255) NOT NULL, " +
                    "author VARCHAR(255) NOT NULL, " +
                    "publication_year INT, " +
                    "is_available BOOLEAN DEFAULT TRUE)");

            stmt.execute("CREATE TABLE IF NOT EXISTS members (" +
                    "member_id INT PRIMARY KEY, " +
                    "name VARCHAR(255) NOT NULL)");

            stmt.execute("CREATE TABLE IF NOT EXISTS borrowing_records (" +
                    "record_id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "member_id INT, " +
                    "isbn VARCHAR(20), " +
                    "borrow_date DATE, " +
                    "return_date DATE, " +
                    "FOREIGN KEY (member_id) REFERENCES members(member_id), " +
                    "FOREIGN KEY (isbn) REFERENCES books(isbn))");
        } catch (SQLException e) {
            System.err.println("Database init: " + e.getMessage());
        }
    }
}
