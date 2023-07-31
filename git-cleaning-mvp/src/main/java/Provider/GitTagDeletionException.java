package Provider;

public class GitTagDeletionException extends Exception {

    public GitTagDeletionException(String message, Throwable error) {
        super(message, error);
    }
}
