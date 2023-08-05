package Business;

import Business.Models.RepoInfo;

import java.util.List;

public interface IGitRepoCleanerLogic {

    void cleanRepos(List<RepoInfo> repos);
}
