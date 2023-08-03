package Business.Models;

public class GitBranchDeletionException extends Exception {

    public GitBranchDeletionException(String message, Throwable error) {
        super(message, error);
    }
}
