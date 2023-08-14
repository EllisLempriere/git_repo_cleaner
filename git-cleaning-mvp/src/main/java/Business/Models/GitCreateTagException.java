package Business.Models;

public class GitCreateTagException extends Exception {

    public GitCreateTagException(String message, Throwable error) {
        super(message, error);
    }

    public GitCreateTagException(String message) {
        super(message);
    }
}
