package Business;

import Business.Models.GitCloningException;
import Business.Models.GitStartupException;
import Business.Models.GitUpdateException;

public interface IGitRepoCleanerLogic {

    void setup() throws GitCloningException, GitUpdateException, GitStartupException;

    void clean() throws GitCloningException, GitUpdateException, GitStartupException;
}
