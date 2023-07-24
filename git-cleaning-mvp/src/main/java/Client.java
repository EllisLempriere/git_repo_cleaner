public class Client {

    public static void main(String[] args) {
        String configFile = args[0];
        Config config = new Config(configFile);

        GitCleaner cleaner = new GitCleaner(config, new LogWrapper());
        cleaner.clean();
    }
}
