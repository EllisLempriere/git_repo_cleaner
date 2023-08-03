package Application;

import Business.*;
import Provider.EmailProvider;
import Provider.GitProvider;
import Provider.IEmailProvider;
import Provider.IGitProvider;

import java.util.logging.Level;

public class Main {

    public static void main(String[] args) {
        ICustomLogger logger = new CustomLogger();
        logger.log(Level.INFO, "Beginning program");

        try {
            logger.log(Level.INFO, "Starting set up");

            Configs configs = new Configs("default_config.properties");

            IGitProvider gitProvider = new GitProvider(configs.REPO_DIR, configs.REMOTE_URI,
                    configs.CONFIG_SECRETS, configs.RETRIES);
            IEmailProvider emailProvider = new EmailProvider();
            INotificationHandler notificationHandler = new NotificationHandler(
                    configs.DAYS_TO_STALE_BRANCH, configs.DAYS_TO_STALE_TAG, configs.PRECEDING_DAYS_TO_WARN, emailProvider);
            IGitRepoCleanerLogic gitRepoCleanerLogic = new GitRepoCleanerLogic(
                    configs.DAYS_TO_STALE_BRANCH, configs.DAYS_TO_STALE_TAG, configs.PRECEDING_DAYS_TO_WARN,
                    configs.EXCLUDED_BRANCHES, configs.REPO_DIR,
                    gitProvider, notificationHandler, logger);
            logger.log(Level.INFO, "Finished setup");

            GitRepoCleaner gitRepoCleaner = new GitRepoCleaner(gitRepoCleanerLogic, logger);
            gitRepoCleaner.clean();

        } catch (ConfigsSetupException e) {
            logger.log(Level.SEVERE, e.getMessage());
            logger.log(Level.SEVERE, "Halting execution due to failed config set up");
        }
    }
}
