package Business.Models;

public class GitSetTagException extends Exception {

    public GitSetTagException(String message, Throwable error) {
        super(message, error);
    }
}
