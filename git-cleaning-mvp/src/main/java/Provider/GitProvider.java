package Provider;

import Application.Models.ConfigSecrets;
import Business.Models.*;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ListBranchCommand;
import org.eclipse.jgit.api.LogCommand;
import org.eclipse.jgit.api.MergeCommand;
import org.eclipse.jgit.api.errors.*;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class GitProvider implements IGitProvider {

    private final CredentialsProvider CREDENTIALS;
    private final int RETRIES;
    private Repository repo;
    private Git git;
    private boolean initialized;

    public GitProvider(ConfigSecrets user, int retries) {
        this.CREDENTIALS = new UsernamePasswordCredentialsProvider(user.USERNAME, user.PASSWORD);
        this.RETRIES = retries;
        this.initialized = false;
    }


    private void initialize(String repoDir) throws GitStartupException {
        String repoDirectory = repoDir + ".git";

        int count = 0;
        while (true) {
            FileRepositoryBuilder repoBuilder = new FileRepositoryBuilder();
            try (Repository repo = repoBuilder
                    .setGitDir(new File(repoDirectory))
                    .readEnvironment()
                    .findGitDir()
                    .build();
                Git git = new Git(repo)) {

                this.repo = repo;
                this.git = git;
                this.initialized = true;

                return;

            } catch (IOException e) {
                if (++count == RETRIES) {
                    throw new GitStartupException(
                            String.format("Failed to start up git due to '%s'", e.getMessage()), e);
                }
            }
        }
    }

    @Override
    public void setupRepo(String repoDir, String remoteUri) {

    }

    @Override
    public void cloneRepo(String repoDir, String remoteUri) throws GitCloningException {
        int count = 0;
        while (true) {
            try (Git ignored = Git.cloneRepository()
                    .setURI(remoteUri)
                    .setRemote("origin")
                    .setDirectory(new File(repoDir))
                    .setCloneAllBranches(true)
                    .setCredentialsProvider(CREDENTIALS)
                    .call()) {

                initialize(repoDir);

                return;

            } catch (GitAPIException e) {
                if (++count == RETRIES) {
                    throw new GitCloningException(
                            String.format("Failed to clone repo after %d attempts due to %s", RETRIES, e.getMessage()),
                            e);
                }
            } catch (GitStartupException e) {
                // TODO - Handle
                throw new RuntimeException(e);
            }
        }
    }

    // TODO - Account for empty repo
    @Override
    public void updateRepo(String repoDir) throws GitUpdateException, GitStartupException {
        if (!initialized)
            initialize(repoDir);

        int count = 0;
        while (true) {
            try {
                // TODO - Bug on retry
                git.pull()
                        .setFastForward(MergeCommand.FastForwardMode.FF_ONLY)
                        .setCredentialsProvider(CREDENTIALS)
                        .call();

                List<Ref> remoteBranches = git.branchList()
                        .setListMode(ListBranchCommand.ListMode.REMOTE)
                        .call();
                List<Ref> localBranches = git.branchList()
                        .call();

                List<Ref> remoteOnlyBranches = getRemoteOnlyBranches(remoteBranches, localBranches);
                for (Ref b : remoteOnlyBranches) {
                    git.checkout().setName(b.getName()).call();
                    git.branchCreate().setName(getRefName(b)).call();
                }

                String trunkBranch = getRefName(remoteBranches.get(remoteBranches.size() - 1));
                git.checkout().setName(trunkBranch).call();

                return;

            } catch (GitAPIException e) {
                if (++count == RETRIES) {
                    throw new GitUpdateException(
                            String.format("Failed to update local repo due to '%s'", e.getMessage()), e);
                }
            }
        }
    }

    @Override
    public List<Branch> getBranches() throws GitBranchFetchException, GitStartupException {
//        if (!initialized)
//            initialize();

        List<Branch> branches = new ArrayList<>();

        int count = 0;
        while (true) {
            try {
                List<Ref> rawBranches = git.branchList().call();
                for (Ref b : rawBranches) {
                    String branchName = getRefName(b);
                    List<Commit> commits = getBranchCommitList(branchName);

                    branches.add(new Branch(branchName, commits));
                }

                return branches;

            } catch (GitAPIException | IOException e) {
                if (++count == RETRIES) {
                    throw new GitBranchFetchException(
                            String.format("Failed to get branch list due to '%s'", e.getMessage()), e);
                }
            }
        }
    }

    @Override
    public List<Tag> getTags() throws GitTagFetchException, GitStartupException {
//        if (!initialized)
//            initialize();

        List<Tag> tags = new ArrayList<>();

        int count = 0;
        while (true) {
            try {
                List<Ref> rawTags = git.tagList().call();

                for (Ref t : rawTags) {
                    String tagName = getRefName(t);
                    Commit commit = getTagCommit(t);

                    tags.add(new Tag(tagName, commit));
                }

                return tags;

            } catch (GitAPIException | IOException e) {
                if (++count == RETRIES) {
                    throw new GitTagFetchException(
                            String.format("Failed to get tag list due to '%s'", e.getMessage()), e);
                }
            }
        }
    }

    @Override
    public void createTag(Tag tag) throws GitSetTagException, GitStartupException {
//        if (!initialized)
//            initialize();

        int count = 0;
        while (true) {
            try {
                RevCommit commit = git.log().add(repo.resolve(tag.commit().commitId())).call().iterator().next();
                git.tag().setName(tag.name()).setObjectId(commit).call();

                return;

            } catch (GitAPIException | IOException e) {
                if (++count == RETRIES) {
                    throw new GitSetTagException(
                            String.format("Failed to set tag %s because %s", tag.name(), e.getMessage()), e);
                }
            }
        }
    }

    @Override
    public void deleteBranch(Branch branch) throws GitBranchDeletionException, GitStartupException {
//        if (!initialized)
//            initialize();

        int count = 0;
        while (true) {
            try {
                git.branchDelete().setBranchNames(branch.name()).setForce(true).call();

                return;

            } catch (GitAPIException e) {
                if (++count == RETRIES) {
                    throw new GitBranchDeletionException(
                            String.format("Failed to delete branch %s because %s", branch.name(), e.getMessage()), e);
                }
            }
        }
    }

    @Override
    public void deleteTag(Tag tag) throws GitTagDeletionException, GitStartupException {
//        if (!initialized)
//            initialize();

        int count = 0;
        while (true) {
            try {
                git.tagDelete().setTags(tag.name()).call();

                return;

            } catch (GitAPIException e) {
                if (++count == RETRIES) {
                    throw new GitTagDeletionException(
                            String.format("Failed to delete tag %s because %s", tag.name(), e.getMessage()), e);
                }
            }
        }
    }

    @Override
    public void pushDeleteRemoteBranch(Branch branch) throws GitPushBranchDeletionException, GitStartupException {
//        if (!initialized)
//            initialize();

        int count = 0;
        while (true) {
            try {
                RefSpec refSpec = new RefSpec()
                        .setSource(null)
                        .setDestination("refs/heads/" + branch.name());

                git.push()
                        .setRefSpecs(refSpec)
                        .setRemote("origin")
                        .setCredentialsProvider(CREDENTIALS)
                        .call();

                return;

            } catch (GitAPIException e) {
                if (++count == RETRIES) {
                    throw new GitPushBranchDeletionException(
                            String.format("Failed to push deletion of branch %s because %s",
                            branch.name(), e.getMessage()), e);
                }
            }
        }
    }

    @Override
    public void pushNewTags() throws GitPushNewTagsException, GitStartupException {
//        if (!initialized)
//            initialize();

        int count = 0;
        while (true) {
            try {
                List<Ref> tags = git.tagList().call();

                if (!tags.isEmpty())
                    git.push()
                            .setPushTags()
                            .setRemote("origin")
                            .setCredentialsProvider(CREDENTIALS)
                            .call();

                return;

            } catch (GitAPIException e) {
                if (++count == RETRIES) {
                    throw new GitPushNewTagsException(
                            String.format("Failed to push new tags to remote because %s",
                            e.getMessage()), e);
                }
            }
        }
    }

    @Override
    public void pushDeleteRemoteTag(Tag tag) throws GitPushTagDeletionException, GitStartupException {
//        if (!initialized)
//            initialize();

        int count = 0;
        while (true) {
            try {
                RefSpec refSpec = new RefSpec()
                        .setSource(null)
                        .setDestination("refs/tags/" + tag.name());

                git.push()
                        .setRefSpecs(refSpec)
                        .setRemote("origin")
                        .setCredentialsProvider(CREDENTIALS)
                        .call();

                return;

            } catch (GitAPIException e) {
                if (++count == RETRIES) {
                    throw new GitPushTagDeletionException(
                            String.format("Failed to push deletion of tag %s to remote because %s",
                            tag.name(), e.getMessage()), e);
                }
            }
        }
    }


    private String getRefName(Ref ref) {
        String[] refPath = ref.getName().split("/");
        return refPath[refPath.length - 1];
    }

    private List<Ref> getRemoteOnlyBranches(List<Ref> remoteBranches, List<Ref> localBranches) {
        List<Ref> onlyRemoteBranches = new ArrayList<>();

        for (Ref rb : remoteBranches) {
            boolean branchOnBoth = false;
            String remoteBranchName = getRefName(rb);
            for (Ref lb : localBranches) {
                String localBranchName = getRefName(lb);
                if (remoteBranchName.equals(localBranchName)) {
                    branchOnBoth = true;
                    break;
                }
            }
            if (!branchOnBoth)
                onlyRemoteBranches.add(rb);
        }

        return onlyRemoteBranches;
    }

    private List<Commit> getBranchCommitList(String branchName) throws IOException, GitAPIException {
        List<Commit> commits = new ArrayList<>();

        Iterable<RevCommit> rawCommits = git.log().add(repo.resolve(branchName)).call();
        for (RevCommit c : rawCommits)
            commits.add(parseRevCommit(c));

        return commits;
    }

    private Commit getTagCommit(Ref tag) throws IOException, GitAPIException {
        LogCommand logCommand = git.log();

        Ref peeledRef = repo.getRefDatabase().peel(tag);
        if (peeledRef.getPeeledObjectId() != null)
            logCommand.add(peeledRef.getPeeledObjectId());
        else
            logCommand.add(tag.getObjectId());

        Iterable<RevCommit> tagLog = logCommand.call();

        return parseRevCommit(tagLog.iterator().next());
    }

    private Commit parseRevCommit(RevCommit rawCommit) {
        String commitId = rawCommit.getId().getName();

        int commitTime = (int) rawCommit.getAuthorIdent().getWhenAsInstant().getEpochSecond();

        String authorEmail = rawCommit.getAuthorIdent().getEmailAddress();

        return new Commit(commitId, commitTime, authorEmail);
    }
}
