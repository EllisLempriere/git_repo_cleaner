package Business.Models;

public class GitCloningException extends Exception {

    public GitCloningException(String message, Throwable error) {
        super(message, error);
    }

    public GitCloningException(String message) {
        super(message);
    }
}
