package Application.Models;

public class ConfigsSetupException extends Exception {

    public ConfigsSetupException(String message, Throwable error) {
        super(message, error);
    }

    public ConfigsSetupException(String message) {
        super(message);
    }
}
