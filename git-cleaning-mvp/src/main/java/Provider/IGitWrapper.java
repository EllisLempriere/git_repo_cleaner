package Provider;

import Business.Branch;
import Business.Tag;

import java.util.List;

public interface IGitWrapper {

    void startGit() throws GitStartupException;

    void updateRepo() throws GitUpdateException;

    List<Branch> getBranches() throws GitBranchFetchException;

    List<Tag> getTags() throws GitTagFetchException;

    void setTag(Tag tag) throws GitSetTagException;

    void deleteBranch(Branch branch) throws GitBranchDeletionException;

    void deleteTag(Tag tag) throws GitTagDeletionException;

    void pushDeleteRemoteBranch(Branch branch) throws GitPushBranchDeletionException;

    void pushNewTags() throws GitPushNewTagsException;

    void pushDeleteRemoteTag(Tag tag) throws GitPushTagDeletionException;
}
