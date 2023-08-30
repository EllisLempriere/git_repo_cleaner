package Business;

import Business.Models.GitCloningException;
import Business.Models.GitStartupException;
import Business.Models.GitUpdateException;
import Business.Models.RepoCleaningInfo;

public interface IGitRepoCleanerLogic {

    void cleanRepos();

    void selectRepo(String repoDir, String remoteUri, int repoNum)
            throws GitCloningException, GitUpdateException, GitStartupException;

    void cleanRepo(RepoCleaningInfo repoCleaningInfo, int repoNum);
}
