package Business.Models;

public class GitTagFetchException extends Exception {

    public GitTagFetchException(String message, Throwable error) {
        super(message, error);
    }

    public GitTagFetchException(String message) {
        super(message);
    }
}
