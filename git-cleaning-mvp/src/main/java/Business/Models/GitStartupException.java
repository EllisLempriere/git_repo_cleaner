package Business.Models;

public class GitStartupException extends Exception {

    public GitStartupException(String message, Throwable error) {
        super(message, error);
    }
}
