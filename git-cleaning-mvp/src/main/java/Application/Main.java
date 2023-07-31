package Application;

import Business.EmailHandler;
import Business.GitCleaner;
import Business.IEmailHandler;
import Provider.GitCloner;
import Provider.GitCloningException;
import Provider.GitWrapper;
import Provider.IGitWrapper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Level;

public class Main {

    public static void main(String[] args) {
        ICustomLogger logger = new CustomLogger();
        logger.log(Level.INFO, "Beginning program");

        try {
            logger.log(Level.INFO, "Starting set up");

            Config config = new Config("default_config.properties");

            logger.log(Level.INFO, "Checking if local repo exists");
            if (!localRepoExist(config.REPO_DIR)) {
                logger.log(Level.INFO, "Local repo does not exist, cloning from remote");

                GitCloner cloner = new GitCloner(config.REPO_DIR, config.REMOTE_URI, config.USER_INFO, config.RETRIES);
                cloner.cloneRepo();

                logger.log(Level.INFO,
                        String.format("Repo successfully cloned from %s to local at %s",
                        config.REMOTE_URI, config.REPO_DIR.substring(0, config.REPO_DIR.length() - 5)));
            } else
                logger.log(Level.INFO, "Local repo exists, continuing execution");


            logger.log(Level.INFO, "Starting cleaning");
            IGitWrapper git = new GitWrapper(config.REPO_DIR, config.USER_INFO, config.RETRIES);
            IEmailHandler mail = new EmailHandler(config.PRECEDING_DAYS_TO_WARN);

            GitCleaner cleaner = new GitCleaner(config.DAYS_TO_STALE_BRANCH, config.DAYS_TO_STALE_TAG,
                    config.PRECEDING_DAYS_TO_WARN, config.EXCLUDED_BRANCHES, git, mail, logger);
            cleaner.clean();
            logger.log(Level.INFO, "Successfully finished cleaning, quitting program");

        } catch (ConfigSetupException e) {
            logger.log(Level.SEVERE, e.getMessage());
            logger.log(Level.SEVERE, "Halting execution due to failed config set up");
        } catch (GitCloningException e) {
            logger.log(Level.SEVERE, e.getMessage());
            logger.log(Level.SEVERE, "Halting execution due to failed git cloning");
        }
    }

    private static boolean localRepoExist(String repoDirectory) {
        Path gitFolderPath = Paths.get(repoDirectory);
        Path directoryPath = Paths.get(repoDirectory.substring(0, repoDirectory.length() - 5));

        return Files.exists(directoryPath) && Files.exists(gitFolderPath);
    }
}
