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
import org.eclipse.jgit.transport.URIish;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;


public class GitProvider implements IGitProvider {

    private final CredentialsProvider CREDENTIALS;
    private final int RETRIES;
    private Repository repo;
    private Git git;

    public GitProvider(ConfigSecrets secrets, int retries) {
        if (secrets == null)
            throw new IllegalArgumentException("User secrets cannot be null");
        if (secrets.USERNAME == null || secrets.PASSWORD == null)
            throw new IllegalArgumentException("Contents of secrets cannot be null");
        if (retries < 0)
            throw new IllegalArgumentException("retries must be >= 0");

        this.CREDENTIALS = new UsernamePasswordCredentialsProvider(secrets.USERNAME, secrets.PASSWORD);
        this.RETRIES = retries;
    }


    @Override
    public void setupRepo(String repoDir) throws GitStartupException {
        if (repoDir == null)
            throw new IllegalArgumentException("Repo directory cannot be null");

        String repoDirectory = repoDir + "\\.git";

        int count = 0;
        while (true) {
            FileRepositoryBuilder repoBuilder = new FileRepositoryBuilder();
            try (Repository repo = repoBuilder
                    .setGitDir(new File(repoDirectory))
                    .readEnvironment()
                    .findGitDir()
                    .setMustExist(true)
                    .build();
                 Git git = new Git(repo)) {

                this.repo = repo;
                this.git = git;

                return;

            } catch (IOException e) {
                if (++count >= RETRIES) {
                    throw new GitStartupException(
                            String.format("Failed to start up git due to: '%s'", e.getMessage()), e);
                }
            }
        }
    }

    @Override
    public void cloneRepo(String repoDir, String remoteUri) throws GitCloningException {
        // TODO - Should repoDir have requirements on it for filepath?
        if (repoDir == null)
            throw new IllegalArgumentException("Repo directory cannot be null");
        if (remoteUri == null)
            throw new IllegalArgumentException("Remote uri cannot be null");

        int count = 0;
        while (true) {
            try (Git git = Git.cloneRepository()
                    .setURI(remoteUri)
                    .setRemote("origin")
                    .setDirectory(new File(repoDir))
                    .setCloneAllBranches(true)
                    .setCredentialsProvider(CREDENTIALS)
                    .call()) {

                git.close();

                return;

            } catch (GitAPIException e) {
                if (++count >= RETRIES) {
                    throw new GitCloningException(
                            String.format("Failed to clone repo after %d attempts due to: '%s'", RETRIES, e.getMessage()), e);
                }
            } catch (JGitInternalException e) {
                throw new GitCloningException(
                        String.format("Cannot clone repo to directory '%s' as it already exists with contents", repoDir), e);
            }
        }
    }

    @Override
    public void updateRepo(String repoDir) throws GitUpdateException, GitStartupException {
        if (git == null)
            setupRepo(repoDir);

        int count = 0;
        while (true) {
            try {
                git.fetch()
                        .setRemoveDeletedRefs(true)
                        .setRemote("origin")
                        .setCredentialsProvider(CREDENTIALS)
                        .call();

                List<Ref> remoteBranches = git.branchList()
                        .setListMode(ListBranchCommand.ListMode.REMOTE)
                        .call();
                List<Ref> localBranches = git.branchList()
                        .call();
                List<Ref> localOnlyBranches = getLocalOnlyBranches(remoteBranches, localBranches);

                if (remoteBranches.size() == 0 && localBranches.size() == 0)
                    return;

                for (Ref b : localOnlyBranches) {
                    if (repo.getBranch().equals(getRefName(b))) {
                        String trunkBranch = getRefName(remoteBranches.get(remoteBranches.size() - 1));
                        git.checkout().setName(trunkBranch).call();
                    }

                    git.branchDelete()
                            .setBranchNames(b.getName())
                            .setForce(true)
                            .call();
                }

                git.pull()
                        .setFastForward(MergeCommand.FastForwardMode.FF_ONLY)
                        .setCredentialsProvider(CREDENTIALS)
                        .call();

                remoteBranches = git.branchList()
                        .setListMode(ListBranchCommand.ListMode.REMOTE)
                        .call();
                localBranches = git.branchList()
                        .call();

                List<Ref> remoteOnlyBranches = getRemoteOnlyBranches(remoteBranches, localBranches);
                for (Ref b : remoteOnlyBranches) {
                    if (b.getName().substring("refs/remotes/origin/".length()).equals("HEAD"))
                        continue;
                    git.checkout().setName(b.getName()).call();
                    git.branchCreate().setName(getRefName(b)).call();
                }

                String trunkBranch = getRefName(remoteBranches.get(remoteBranches.size() - 1));
                if (!repo.getBranch().equals(trunkBranch))
                    git.checkout().setName(trunkBranch).call();

                return;

            } catch (GitAPIException | IOException e) {
                if (++count >= RETRIES) {
                    throw new GitUpdateException(
                            String.format("Failed to update local repo due to: '%s'", e.getMessage()), e);
                }
            }
        }
    }

    @Override
    public List<Branch> getBranches() throws GitBranchFetchException, GitNotSetupException {
        if (git == null)
            throw new GitNotSetupException("Git not started up, call setupRepo first");

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
                if (++count >= RETRIES) {
                    throw new GitBranchFetchException(
                            String.format("Failed to get branch list due to: '%s'", e.getMessage()), e);
                }
            }
        }
    }

    @Override
    public List<Tag> getTags() throws GitTagFetchException, GitNotSetupException {
        if (git == null)
            throw new GitNotSetupException("Git not started up, call setupRepo first");

        List<Tag> tags = new ArrayList<>();

        int count = 0;
        while (true) {
            try {
                List<Ref> rawTags = git.tagList().call();

                for (Ref t : rawTags) {
                    String tagName = getRefName(t);
                    List<Commit> commits = getTagCommits(t);

                    tags.add(new Tag(tagName, commits));
                }

                return tags;

            } catch (GitAPIException | IOException e) {
                if (++count >= RETRIES) {
                    throw new GitTagFetchException(
                            String.format("Failed to get tag list due to: '%s'", e.getMessage()), e);
                }
            }
        }
    }

    @Override
    public void createTag(Tag tag) throws GitCreateTagException {
        if (tag == null)
            throw new IllegalArgumentException("Tag to create cannot be null");
        if (git == null)
            throw new GitNotSetupException("Git not started up, call setupRepo first");
        if (tag.name() == null)
            throw new IllegalArgumentException("Tag name cannot be null");
        if (tag.commits() == null || tag.commits().isEmpty())
            throw new IllegalArgumentException("Tag must have commits with contents");

        int count = 0;
        while (true) {
            try {
                Iterable<RevCommit> rawCommits = git.log().add(repo.resolve(tag.commits().get(0).commitId())).call();
                ArrayList<RevCommit> commits = new ArrayList<>();
                rawCommits.forEach(commits::add);

                if (commits.size() != tag.commits().size())
                    throw new GitCreateTagException("Tag commits must contain full commit history");
                for (int i = 0; i < commits.size(); i++)
                    if (!parseRevCommit(commits.get(i)).equals(tag.commits().get(i)))
                        throw new GitCreateTagException("Tag commits must match correct commit history");

                git.tag().setName(tag.name()).setObjectId(commits.get(0)).call();

                return;

            } catch (GitAPIException | IOException | NullPointerException e) {
                if (++count >= RETRIES) {
                    throw new GitCreateTagException(
                            String.format("Failed to set tag %s because: '%s'", tag.name(), e.getMessage()), e);
                }
            }
        }
    }

    @Override
    public void deleteBranch(Branch branch) throws GitBranchDeletionException {
        if (branch == null)
            throw new IllegalArgumentException("Cannot delete null branch");
        if (git == null)
            throw new GitNotSetupException("Git not started up, call setupRepo first");

        int count = 0;
        while (true) {
            try {
                List<Ref> branches = git.branchList().call();
                String trunkBranch = getRefName(branches.get(branches.size() - 1));
                if (repo.getBranch().equals(branch.name()))
                    git.checkout().setName(trunkBranch).call();

                git.branchDelete().setBranchNames(branch.name()).setForce(true).call();

                return;

            } catch (GitAPIException | IOException e) {
                if (++count >= RETRIES) {
                    throw new GitBranchDeletionException(
                            String.format("Failed to delete branch %s because: '%s'", branch.name(), e.getMessage()), e);
                }
            }
        }
    }

    @Override
    public void deleteTag(Tag tag) throws GitTagDeletionException {
        if (tag == null)
            throw new IllegalArgumentException("Cannot delete null branch");
        if (git == null)
            throw new GitNotSetupException("Git not started up, call setupRepo first");

        int count = 0;
        while (true) {
            try {
                git.tagDelete().setTags(tag.name()).call();

                return;

            } catch (GitAPIException e) {
                if (++count >= RETRIES) {
                    throw new GitTagDeletionException(
                            String.format("Failed to delete tag %s because: '%s'", tag.name(), e.getMessage()), e);
                }
            }
        }
    }

    @Override
    public void pushDeleteRemoteBranch(Branch branch) throws GitPushBranchDeletionException {
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
                if (++count >= RETRIES) {
                    throw new GitPushBranchDeletionException(
                            String.format("Failed to push deletion of branch %s because: '%s'",
                            branch.name(), e.getMessage()), e);
                }
            }
        }
    }

    @Override
    public void pushNewTags() throws GitPushNewTagsException {
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
                if (++count >= RETRIES) {
                    throw new GitPushNewTagsException(
                            String.format("Failed to push new tags to remote because: '%s'",
                            e.getMessage()), e);
                }
            }
        }
    }

    @Override
    public void pushDeleteRemoteTag(Tag tag) throws GitPushTagDeletionException {
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
                if (++count >= RETRIES) {
                    throw new GitPushTagDeletionException(
                            String.format("Failed to push deletion of tag %s to remote because: '%s'",
                            tag.name(), e.getMessage()), e);
                }
            }
        }
    }

    @Override
    public void addRemote(String remoteUri) {
        int count = 0;
        while (true) {
            try {
                git.remoteAdd()
                        .setUri(new URIish(remoteUri))
                        .setName("origin")
                        .call();

                return;

            } catch (GitAPIException | URISyntaxException e) {
                if (++count >= RETRIES)
                    throw new RuntimeException();
            }
        }
    }

    @Override
    public void removeRemote(String remoteName) {
        int count = 0;
        while (true) {
            try {
                git.remoteRemove()
                        .setRemoteName(remoteName)
                        .call();

                return;

            } catch (GitAPIException e) {
                if (++count >= RETRIES)
                    throw new RuntimeException();
            }
        }
    }

    @Override
    public void checkoutBranch(String branchName) {
        int count = 0;
        while (true) {
            try {
                git.checkout().setCreateBranch(false).setName(branchName).call();

                return;

            } catch (GitAPIException e) {
                if (++count >= RETRIES)
                    throw new RuntimeException();
            }
        }
    }

    public void shutdownRepo() {
        if (this.git != null)
            this.git.getRepository().close();
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

    private List<Ref> getLocalOnlyBranches(List<Ref> remoteBranches, List<Ref> localBranches) {
        List<Ref> onlyLocalBranches = new ArrayList<>();

        for (Ref lb : localBranches) {
            boolean branchOnBoth = false;
            String localBranchName = getRefName(lb);
            for (Ref rm : remoteBranches) {
                String remoteBranchName = getRefName(rm);
                if (localBranchName.equals(remoteBranchName)) {
                    branchOnBoth = true;
                    break;
                }
            }
            if (!branchOnBoth)
                onlyLocalBranches.add(lb);
        }

        return onlyLocalBranches;
    }

    private List<Commit> getBranchCommitList(String branchName) throws IOException, GitAPIException {
        List<Commit> commits = new ArrayList<>();

        Iterable<RevCommit> rawCommits = git.log().add(repo.resolve(branchName)).call();
        for (RevCommit c : rawCommits)
            commits.add(parseRevCommit(c));

        return commits;
    }

    private List<Commit> getTagCommits(Ref tag) throws IOException, GitAPIException {
        List<Commit> commits = new ArrayList<>();

        LogCommand logCommand = git.log();

        Ref peeledRef = repo.getRefDatabase().peel(tag);
        if (peeledRef.getPeeledObjectId() != null)
            logCommand.add(peeledRef.getPeeledObjectId());
        else
            logCommand.add(tag.getObjectId());

        Iterable<RevCommit> rawCommits = logCommand.call();
        for (RevCommit c : rawCommits)
            commits.add(parseRevCommit(c));

        return commits;
    }

    private Commit parseRevCommit(RevCommit rawCommit) {
        String commitId = rawCommit.getId().getName();

        int commitTime = (int) rawCommit.getAuthorIdent().getWhenAsInstant().getEpochSecond();

        String authorEmail = rawCommit.getAuthorIdent().getEmailAddress();

        return new Commit(commitId, commitTime, authorEmail);
    }
}
