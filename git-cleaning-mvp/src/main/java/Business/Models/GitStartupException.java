package Business.Models;

public class GitStartupException extends Exception {

    public GitStartupException(String message, Throwable error) {
        super(message, error);
    }

    public GitStartupException(String message) {
        super(message);
    }
}
