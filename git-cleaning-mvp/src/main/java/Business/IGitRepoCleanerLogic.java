package Business;

import Business.Models.GitCloningException;
import Business.Models.GitStartupException;
import Business.Models.GitUpdateException;

public interface IGitRepoCleanerLogic {

    void cleanRepos();

    void selectRepo() throws GitCloningException, GitUpdateException, GitStartupException;

    void cleanRepo() throws GitCloningException, GitUpdateException, GitStartupException;
}
