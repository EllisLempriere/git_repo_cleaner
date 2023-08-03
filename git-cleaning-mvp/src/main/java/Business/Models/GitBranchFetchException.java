package Business.Models;

public class GitBranchFetchException extends Exception {

    public GitBranchFetchException(String message, Throwable error) {
        super(message, error);
    }
}
