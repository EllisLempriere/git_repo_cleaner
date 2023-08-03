package Application;

import Business.*;
import Provider.EmailProvider;
import Provider.GitRepo;
import Provider.IEmailProvider;
import Provider.IGitRepo;

import java.util.logging.Level;

public class Main {

    public static void main(String[] args) {
        ICustomLogger logger = new CustomLogger();
        logger.log(Level.INFO, "Beginning program");

        try {
            logger.log(Level.INFO, "Starting set up");

            Config config = new Config("default_config.properties");

            logger.log(Level.INFO, "Setting up local repo");
            IRepoSetup setup = new RepoSetup(config.REPO_DIR, config.REMOTE_URI, config.USER_INFO, config.RETRIES, logger);
            if (!setup.setup())
                return;

            logger.log(Level.INFO, "Starting cleaning");
            IGitRepo git = new GitRepo(config.REPO_DIR, config.USER_INFO, config.RETRIES);
            IEmailProvider email = new EmailProvider();
            INotificationHandler notifications = new NotificationHandler(
                    config.DAYS_TO_STALE_BRANCH, config.DAYS_TO_STALE_TAG, config.PRECEDING_DAYS_TO_WARN, email);

            IGitCleaner cleaner = new GitCleaner(config.DAYS_TO_STALE_BRANCH, config.DAYS_TO_STALE_TAG,
                    config.PRECEDING_DAYS_TO_WARN, config.EXCLUDED_BRANCHES, git, notifications, logger);
            cleaner.clean();
            logger.log(Level.INFO, "Successfully finished cleaning, quitting program");

        } catch (ConfigSetupException e) {
            logger.log(Level.SEVERE, e.getMessage());
            logger.log(Level.SEVERE, "Halting execution due to failed config set up");
        }
    }
}
