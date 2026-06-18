public class BorrowTask implements Runnable {
    private final LibraryService libraryService;
    private final Member member;
    private final String isbn;

    public BorrowTask(LibraryService libraryService, Member member, String isbn) {
        this.libraryService = libraryService;
        this.member = member;
        this.isbn = isbn;
    }

    @Override
    public void run() {
        System.out.println("[Thread " + Thread.currentThread().getName() + "] Processing borrow request for " + member.getName() + " -> ISBN: " + isbn);
        libraryService.borrowBook(member, isbn);
    }
}
