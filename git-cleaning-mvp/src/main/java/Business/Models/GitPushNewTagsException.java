package Business.Models;

public class GitPushNewTagsException extends Exception {

    public GitPushNewTagsException(String message, Throwable error) {
        super(message, error);
    }
}
