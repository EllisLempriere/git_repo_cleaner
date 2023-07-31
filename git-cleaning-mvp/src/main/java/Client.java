// Program or Main
public class Client {

    public static void main(String[] args) {
        String configFile = args[0]; // remove
        Config config = new Config(configFile);
        ILogWrapper logger = new LogWrapper(); //ICustomLogger, CustomLogger

        // Dependency injections should happen in constructor
        // Break out instantiations
        // Pull cloner to application layer
        GitCleaner cleaner = new GitCleaner(config, logger, new GitCloner(config));
        cleaner.clean(new GitWrapper(config), new EmailHandler(config));
    }
}

// Change directory structure
