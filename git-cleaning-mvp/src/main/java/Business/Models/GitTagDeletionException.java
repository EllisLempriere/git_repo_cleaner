package Business.Models;

public class GitTagDeletionException extends Exception {

    public GitTagDeletionException(String message, Throwable error) {
        super(message, error);
    }

    public GitTagDeletionException(String message) {
        super(message);
    }
}
