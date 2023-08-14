package Business.Models;

public class GitBranchFetchException extends Exception {

    public GitBranchFetchException(String message, Throwable error) {
        super(message, error);
    }

    public GitBranchFetchException(String message) {
        super(message);
    }
}
