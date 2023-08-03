package Business;

import Application.ICustomLogger;
import Application.UserCredentials;
import Business.Models.GitCloningException;
import Provider.GitCloner;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Level;

public class RepoSetup implements IRepoSetup {

    private final String REPO_DIR;
    private final String REMOTE_URI;
    private final UserCredentials USER;
    private final int RETRIES;
    private final ICustomLogger LOGGER;

    public RepoSetup(String repoDir, String remoteUri, UserCredentials user, int retries, ICustomLogger log) {
        this.REPO_DIR = repoDir;
        this.REMOTE_URI = remoteUri;
        this.USER = user;
        this.RETRIES = retries;
        this.LOGGER = log;
    }


    @Override
    public boolean setup() {
        try {
            LOGGER.log(Level.INFO, "Checking if local repo exists");
            if (!localRepoExist(REPO_DIR)) {
                LOGGER.log(Level.INFO, "Local repo does not exist, cloning from remote");

                GitCloner cloner = new GitCloner(REPO_DIR, REMOTE_URI, USER, RETRIES);
                cloner.cloneRepo();

                LOGGER.log(Level.INFO,
                        String.format("Repo successfully cloned from %s to local at %s",
                        REMOTE_URI, REPO_DIR.substring(0, REPO_DIR.length() - 5)));
            } else
                LOGGER.log(Level.INFO, "Local repo exists, continuing execution");

            return true;

        } catch (GitCloningException e) {
            LOGGER.log(Level.SEVERE, e.getMessage());
            LOGGER.log(Level.SEVERE, "Halting execution due to failed git cloning");
            return false;
        }
    }


    private static boolean localRepoExist(String repoDirectory) {
        Path gitFolderPath = Paths.get(repoDirectory);
        Path directoryPath = Paths.get(repoDirectory.substring(0, repoDirectory.length() - 5));

        return Files.exists(directoryPath) && Files.exists(gitFolderPath);
    }
}
