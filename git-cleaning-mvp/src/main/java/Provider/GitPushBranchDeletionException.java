package Provider;

public class GitPushBranchDeletionException extends Exception {

    public GitPushBranchDeletionException(String message, Throwable error) {
        super(message, error);
    }
}
