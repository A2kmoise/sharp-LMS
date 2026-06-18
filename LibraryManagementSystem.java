import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

class Book {
    private final String isbn;
    private final String title;
    private final String author;
    private final int publicationYear;
    private final boolean isAvailable;

    public Book(String isbn, String title, String author, int publicationYear, boolean isAvailable) {
        this.isbn = isbn;
        this.title = title;
        this.author = author;
        this.publicationYear = publicationYear;
        this.isAvailable = isAvailable;
    }

    public String getIsbn() { return isbn; }
    public String getTitle() { return title; }
    public String getAuthor() { return author; }
    public int getPublicationYear() { return publicationYear; }
    public boolean isAvailable() { return isAvailable; }

    @Override
    public String toString() {
        return String.format("%-10s | %-25s | %-20s | %-4d", isbn, title, author, publicationYear);
    }
}

class Member {
    private final int id;
    private final String name;

    private final List<Book> borrowedBooks;

    public Member(int id, String name) {
        this.id = id;
        this.name = name;
        this.borrowedBooks = new ArrayList<>();
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public List<Book> getBorrowedBooks() { return borrowedBooks; }

    public void addBorrowedBook(Book book) {
        this.borrowedBooks.add(book);
    }

    public void removeBorrowedBook(String isbn) {
        this.borrowedBooks.removeIf(b -> b.getIsbn().equals(isbn));
    }
}

public class LibraryManagementSystem {
    public static void main(String[] args) {

        // Mysql database credentials(i used local database)
        String dbUrl = "jdbc:mysql://localhost:3306/oop_exam";
        String user = "root";
        String password = "";

        DatabaseConnection db = new DatabaseConnection(dbUrl, user, password);
        db.initializeDatabase();
        
        LibraryService service = new LibraryService(db);
        Scanner scanner = new Scanner(System.in);

        ExecutorService executor = Executors.newFixedThreadPool(3);

        boolean running = true;
        while (running) {
            System.out.printf("==========================================\n");
            System.out.printf("%25s\n", "Rwanda National Digital Library (RNDL)");
            System.out.printf("=========================================\n");
            System.out.printf("1. %-30s\n", "Register new book");
            System.out.printf("2. %-30s\n", "Register new member");
            System.out.printf("3. %-30s\n", "List available books");
            System.out.printf("4. %-30s\n", "Borrow a book");
            System.out.printf("5. %-30s\n", "Return a book");
            System.out.printf("6. %-30s\n", "Simulate concurrent borrowing");
            System.out.printf("7. %-30s\n", "List all books");
            System.out.printf("8. %-30s\n", "Delete a book");
            System.out.printf("9. %-30s\n", "Update a book");
            System.out.printf("10. %-30s\n", "Exit");
            System.out.printf("========================================\n");
            System.out.printf("Choose your option: ");

            String choice = scanner.nextLine();

            switch (choice) {
                case "1":
                    try {
                        System.out.printf("Enter Book ISBN: ");
                        String newIsbn = scanner.nextLine();
                        System.out.printf("Enter Book Title: ");
                        String title = scanner.nextLine();
                        System.out.printf("Enter Book Author: ");
                        String author = scanner.nextLine();
                        System.out.printf("Enter Publication Year: ");
                        int year = Integer.parseInt(scanner.nextLine());
                        int currentYear = java.time.LocalDate.now().getYear();
                        if (year > currentYear) {
                            System.out.printf("> Invalid year: publication year cannot be greater than %d.\n", currentYear);
                            break;
                        }

                        try (Connection conn = db.getConnection();
                             PreparedStatement stmt = conn.prepareStatement("INSERT INTO books (isbn, title, author, publication_year) VALUES (?, ?, ?, ?)")) {
                            stmt.setString(1, newIsbn);
                            stmt.setString(2, title);
                            stmt.setString(3, author);
                            stmt.setInt(4, year);
                            stmt.executeUpdate();
                            System.out.printf("> Book '%s' registered successfully.\n", title);
                        } catch (SQLException e) {
                            System.out.printf("> Failed to register book: %s\n", e.getMessage());
                        }
                    } catch (Exception e) {
                        System.out.printf("> Invalid input. Please try again.\n");
                    }
                    break;
                    
                case "2":
                    try {
                        System.out.printf("Enter Member ID: ");
                        int newMemberId = Integer.parseInt(scanner.nextLine());
                        System.out.printf("Enter Member Name: ");
                        String newName = scanner.nextLine();
                        
                        try (Connection conn = db.getConnection();
                             PreparedStatement stmt = conn.prepareStatement("INSERT INTO members (member_id, name) VALUES (?, ?)")) {
                            stmt.setInt(1, newMemberId);
                            stmt.setString(2, newName);
                            stmt.executeUpdate();
                            System.out.printf("> Member '%s' registered successfully.\n", newName);
                        } catch (SQLException e) {
                            System.out.printf("> Failed to register member: %s\n", e.getMessage());
                        }
                    } catch (Exception e) {
                        System.out.printf("> Invalid input. Please try again.\n");
                    }
                    break;
                    
                case "3":
                    List<Book> books = service.listAvailableBooks();
                    if (books.isEmpty()) {
                        System.out.printf("\n> No books available .\n");
                    } else {
                        System.out.printf("\n Available Books \n");
                        System.out.printf("%-10s | %-25s | %-20s | %-4s\n", "ISBN", "Title", "Author", "Year");
                        System.out.printf("----------------------------------------------------------------------\n");
                        for (Book b : books) {
                            System.out.printf("%s\n", b.toString());
                        }
                        System.out.printf("----------------------------------------------------------------------\n");
                    }
                    break;
                    
                case "4":
                    try {
                        System.out.printf("Enter Member ID: ");
                        int borrowMemberId = Integer.parseInt(scanner.nextLine());
                        System.out.printf("Enter Member Name: ");
                        String borrowMemberName = scanner.nextLine();
                        System.out.printf("Enter Book ISBN to borrow: ");
                        String borrowIsbn = scanner.nextLine();
                        
                        Member borrowMember = new Member(borrowMemberId, borrowMemberName);

                        executor.submit(new BorrowTask(service, borrowMember, borrowIsbn));

                        Thread.sleep(100); 
                    } catch (Exception e) {
                        System.out.printf("> Invalid input. Please try again.\n");
                    }
                    break;
                    
                case "5":
                    try {
                        System.out.printf("Enter Member ID: ");
                        int returnMemberId = Integer.parseInt(scanner.nextLine());
                        System.out.printf("Enter Member Name: ");
                        String returnMemberName = scanner.nextLine();
                        System.out.printf("Enter Book ISBN to return: ");
                        String returnIsbn = scanner.nextLine();
                        
                        Member returnMember = new Member(returnMemberId, returnMemberName);
                        service.returnBook(returnMember, returnIsbn);
                    } catch (Exception e) {
                        System.out.printf("> Invalid input. Please try again.\n");
                    }
                    break;
                    
                case "6":
                    System.out.printf("> Simulating concurrent requests on the exact same book...\n");
                    Member alice = new Member(9, "Moise");
                    Member bob = new Member(10, "Junior");
                    
                    try (Connection conn = db.getConnection();
                         PreparedStatement stmt = conn.prepareStatement("INSERT IGNORE INTO books (isbn, title, author, publication_year) VALUES ('TEST-1', 'Concurrency Test Book', 'System', 2024)")) {
                         stmt.executeUpdate();
                    } catch (SQLException ignored) {}

                    executor.submit(new BorrowTask(service, alice, "TEST-1"));
                    executor.submit(new BorrowTask(service, bob, "TEST-1"));
                    
                    try { Thread.sleep(200); } catch (InterruptedException ignored) {}
                    break;
                    
                case "7":
                    // List all books (including unavailable)
                    List<Book> allBooks = service.listAllBooks();
                    if (allBooks.isEmpty()) {
                        System.out.printf("\n> No books found in the library.\n");
                    } else {
                        System.out.printf("\n All Books \n");
                        System.out.printf("%-10s | %-25s | %-20s | %-4s | %-9s\n", "ISBN", "Title", "Author", "Year", "Available");
                        System.out.printf("---------------------------------------------------------------\n");
                        for (Book b : allBooks) {
                            System.out.printf("%s | %s | %s | %d | %s\n",
                                    b.getIsbn(), b.getTitle(), b.getAuthor(), b.getPublicationYear(), b.isAvailable() ? "Yes" : "No");
                        }
                        System.out.printf("---------------------------------------------------------------\n");
                    }
                    break;
                case "8":
                    // Delete a book by ISBN
                    try {
                        System.out.printf("Enter ISBN of the book to delete: ");
                        String delIsbn = scanner.nextLine();
                        service.deleteBook(delIsbn);
                    } catch (Exception e) {
                        System.out.printf("> Invalid input. Please try again.\n");
                    }
                    break;
                case "9":
                    // Update book details
                    try {
                        System.out.printf("Enter ISBN of the book to update: ");
                        String updIsbn = scanner.nextLine();
                        System.out.printf("Enter new Title (or same to keep): ");
                        String newTitle = scanner.nextLine();
                        System.out.printf("Enter new Author (or same to keep): ");
                        String newAuthor = scanner.nextLine();
                        System.out.printf("Enter new Publication Year: ");
                        int newYear = Integer.parseInt(scanner.nextLine());
                        service.updateBook(updIsbn, newTitle, newAuthor, newYear);
                    } catch (Exception e) {
                        System.out.printf("> Invalid input. Please try again.\n");
                    }
                    break;
                case "10":
                    running = false;
                    System.out.printf("> bye....\n");
                    executor.shutdown();
                    break;
                
                default:
                    System.out.printf("> Invalid option. Please try again.\n");
            }
        }
        // close resources
        scanner.close();
    }
}