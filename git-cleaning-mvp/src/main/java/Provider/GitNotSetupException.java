package Provider;

public class GitNotSetupException extends RuntimeException {

    public GitNotSetupException(String message) {
        super(message);
    }
}
