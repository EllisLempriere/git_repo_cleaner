public class Client {

    public static void main(String[] args) {
        String configFile = args[0];
        Config config = new Config(configFile);
        ILogWrapper logger = new LogWrapper();

        GitCleaner cleaner = new GitCleaner(config, logger, new GitCloner(config));
        cleaner.clean(new GitWrapper(config), new EmailHandler(config));
    }
}
