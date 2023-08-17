package Provider;

import Business.Models.*;

import java.util.List;

public interface IGitProvider {

    void setupRepo(String repoDir) throws GitStartupException;

    void cloneRepo(String repoDir, String remoteUri) throws GitCloningException;

    void updateRepo(String repoDir) throws GitUpdateException, GitStartupException;

    List<Branch> getBranches() throws GitBranchFetchException, GitNotSetupException;

    List<Tag> getTags() throws GitTagFetchException, GitNotSetupException;

    void createTag(Tag tag) throws GitCreateTagException;

    void deleteBranch(Branch branch) throws GitBranchDeletionException;

    void deleteTag(Tag tag) throws GitTagDeletionException;

    void pushDeleteRemoteBranch(Branch branch) throws GitPushBranchDeletionException;

    void pushNewTags() throws GitPushNewTagsException;

    void pushDeleteRemoteTag(Tag tag) throws GitPushTagDeletionException;

    void addRemote(String remoteUri);

    void removeRemote(String remoteName);

    void checkoutBranch(String branchName);
}
