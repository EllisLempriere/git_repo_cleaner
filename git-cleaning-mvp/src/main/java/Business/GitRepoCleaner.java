package Business;

import Application.ICustomLogger;
import Business.Models.GitCloningException;
import Business.Models.GitStartupException;
import Business.Models.GitUpdateException;

import java.util.logging.Level;

public class GitRepoCleaner {

    private final IGitRepoCleanerLogic gitRepoCleanerLogic;
    private final ICustomLogger logger;

    public GitRepoCleaner(IGitRepoCleanerLogic gitRepoCleanerLogic, ICustomLogger logger) {
        this.gitRepoCleanerLogic = gitRepoCleanerLogic;
        this.logger = logger;
    }

    public void clean() {
        try {
            logger.log(Level.INFO, "Setting up local repo");
            gitRepoCleanerLogic.setup();

            logger.log(Level.INFO, "Starting cleaning");
            gitRepoCleanerLogic.clean();
            logger.log(Level.INFO, "Successfully finished cleaning, quitting program");
        } catch (GitCloningException e) {
            logger.log(Level.SEVERE, e.getMessage());
            logger.log(Level.SEVERE, "Halting execution due to failed remote repo clone");
        } catch (GitUpdateException e) {
            logger.log(Level.SEVERE, e.getMessage());
            logger.log(Level.SEVERE, "Halting execution due to failed local repo update");
        } catch (GitStartupException e) {
            logger.log(Level.SEVERE, e.getMessage());
            logger.log(Level.SEVERE, "Halting execution due to failed initialization of git object");
        }
    }
}
