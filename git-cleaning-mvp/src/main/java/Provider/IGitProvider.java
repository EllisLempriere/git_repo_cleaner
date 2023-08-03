package Provider;

import Business.Models.*;

import java.util.List;

public interface IGitProvider {

    void cloneRepo() throws GitCloningException;

    void updateRepo() throws GitUpdateException, GitStartupException;

    List<Branch> getBranches() throws GitBranchFetchException, GitStartupException;

    List<Tag> getTags() throws GitTagFetchException, GitStartupException;

    void createTag(Tag tag) throws GitSetTagException, GitStartupException;

    void deleteBranch(Branch branch) throws GitBranchDeletionException, GitStartupException;

    void deleteTag(Tag tag) throws GitTagDeletionException, GitStartupException;

    void pushDeleteRemoteBranch(Branch branch) throws GitPushBranchDeletionException, GitStartupException;

    void pushNewTags() throws GitPushNewTagsException, GitStartupException;

    void pushDeleteRemoteTag(Tag tag) throws GitPushTagDeletionException, GitStartupException;
}
