package Provider;

public class GitTagFetchException extends Exception {

    public GitTagFetchException(String message, Throwable error) {
        super(message, error);
    }
}
