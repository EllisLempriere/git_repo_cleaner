package Provider;

import Business.Models.*;

import java.util.List;

public interface IGitProvider {

    void setupRepo(String repoDir, String remoteUri);

    void cloneRepo(String repoDir, String remoteUri) throws GitCloningException;

    void updateRepo(String repoDir) throws GitUpdateException, GitStartupException;

    List<Branch> getBranches() throws GitBranchFetchException, GitStartupException;

    List<Tag> getTags() throws GitTagFetchException, GitStartupException;

    void createTag(Tag tag) throws GitSetTagException, GitStartupException;

    void deleteBranch(Branch branch) throws GitBranchDeletionException, GitStartupException;

    void deleteTag(Tag tag) throws GitTagDeletionException, GitStartupException;

    void pushDeleteRemoteBranch(Branch branch) throws GitPushBranchDeletionException, GitStartupException;

    void pushNewTags() throws GitPushNewTagsException, GitStartupException;

    void pushDeleteRemoteTag(Tag tag) throws GitPushTagDeletionException, GitStartupException;
}
