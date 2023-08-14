package Business.Models;

public class GitUpdateException extends Exception {

    public GitUpdateException(String message, Throwable error) {
        super(message, error);
    }

    public GitUpdateException(String message) {
        super(message);
    }
}
