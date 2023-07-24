import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.LogCommand;
import org.eclipse.jgit.api.errors.*;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

// This class will assume the repo exists locally. Another class will take care setting it up if needed
public class GitWrapper implements IGitWrapper {

    private Repository repo;
    private final Git git;


    public GitWrapper(String filePath, ILogWrapper log) {
        log.log(Level.INFO, "Initializing Git");

        File directory = new File(filePath);

        for (int i = 0; i < 3; i++) {
            FileRepositoryBuilder repoBuilder = new FileRepositoryBuilder();
            try (Repository repo = repoBuilder.setGitDir(directory)
                    .readEnvironment()
                    .findGitDir()
                    .build()) {

                this.repo = repo;
                break;

            } catch (IOException e) {
                if (i < 2)
                    log.log(Level.WARNING, "Failed to open repo, attempt: " + (i + 1) +
                            ", because of exception: " + e.getMessage() + ". Trying again");
                else {
                    log.log(Level.SEVERE, "Repo could not be opened because exception: " + e.getMessage());
                    throw new RuntimeException(); // Custom exception?
                }
            }
        }

        try (Git git = new Git(repo)) {
            this.git = git;
        }

        log.log(Level.INFO, "Git successfully initialized");
    }


    @Override
    public List<Branch> getBranches(ILogWrapper log) {
        log.log(Level.INFO, "Fetching branch list");

        List<Branch> branches = new ArrayList<>();

        for (int i = 0; i < 3; i++) {
            try {
                List<Ref> rawBranches = git.branchList().call();

                for (Ref b : rawBranches) {
                    String[] refPath = b.getName().split("/");
                    String branchName = refPath[refPath.length - 1];

                    List<Commit> commits = getBranchCommitList(branchName);

                    branches.add(new Branch(branchName, commits));
                }
                break;

            } catch (GitAPIException | IOException e) {
                if (i < 2)
                    log.log(Level.WARNING, "Failed to fetch branches, attempt: " + (i + 1) +
                            ", because of exception: " + e.getMessage() + ". Trying again");
                else {
                    log.log(Level.SEVERE, "Branches could not be fetched because exception: " + e.getMessage());
                    return null;
                }
            }
        }

        log.log(Level.INFO, "Successfully obtained branch list");
        return branches;
    }

    @Override
    public List<Tag> getTags(ILogWrapper log) {
        log.log(Level.INFO, "Fetching tag list");

        List<Tag> tags = new ArrayList<>();

        for (int i = 0; i < 3; i++) {
            try {
                List<Ref> rawTags = git.tagList().call();

                for (Ref t : rawTags) {
                    String[] refPath = t.getName().split("/");
                    String tagName = refPath[refPath.length - 1];

                    Commit commit = getTagCommit(t);

                    tags.add(new Tag(tagName, commit));
                }
                break;

            } catch (GitAPIException | IOException e) {
                if (i < 2)
                    log.log(Level.WARNING, "Failed to fetch tags, attempt: " + (i + 1) +
                            ", because of exception: " + e.getMessage() + ". Trying again");
                else {
                    log.log(Level.SEVERE, "Tags could not be fetched because exception: " + e.getMessage());
                    return null;
                }
            }
        }

        log.log(Level.INFO, "Successfully obtained tag list");
        return tags;
    }

    @Override
    public boolean setTag(Tag tag, ILogWrapper log) {
        log.log(Level.INFO, "Adding tag " + tag.name());

        for (int i = 0; i < 3; i++) {
            try {
                RevCommit commit = git.log().add(repo.resolve(tag.commit().commitId())).call().iterator().next();

                git.tag().setName(tag.name()).setObjectId(commit).call();

                break;

            } catch (GitAPIException | IOException e) {
                if (i < 2)
                    log.log(Level.WARNING, "Failed to create tag " + tag.name() +
                            " attempt: " + (i + 1) + ", because of exception: " + e.getMessage() + ". Trying again");
                else {
                    log.log(Level.SEVERE, "Tag " + tag.name() +
                            " could not be created because exception: " + e.getMessage());
                    return false;
                }
            }
        }

        log.log(Level.INFO, "Successfully added tag " + tag.name());
        return true;
    }

    @Override
    public void deleteBranch(Branch branch, ILogWrapper log) {
        log.log(Level.INFO, "Deleting branch " + branch.name());

        for (int i = 0; i < 3; i++) {
            try {
                git.branchDelete().setBranchNames(branch.name()).setForce(true).call();

                break;

            } catch (GitAPIException e) {
                if (i < 2)
                    log.log(Level.WARNING, "Failed to delete branch " + branch.name() +
                            " attempt: " + (i + 1) + ", because of exception: " + e.getMessage() + ". Trying again");
                else {
                    log.log(Level.SEVERE, "Branch " + branch.name() +
                            " could not be deleted because exception: " + e.getMessage());
                    throw new RuntimeException(); // Custom exception?
                    // Execution should continue even if branch cannot be deleted
                }
            }
        }

        log.log(Level.INFO, "Successfully deleted branch " + branch.name());
    }

    // Should this return something to confirm tag deletion?
    @Override
    public void deleteTag(Tag tag, ILogWrapper log) {
        log.log(Level.INFO, "Deleting tag " + tag.name());

        for (int i = 0; i < 3; i++) {
            try {
                git.tagDelete().setTags(tag.name()).call();

                break;

            } catch (GitAPIException e) {
                if (i < 2)
                    log.log(Level.WARNING, "Failed to delete tag " + tag.name() +
                            " attempt: " + (i + 1) + ", because of exception: " + e.getMessage() + ". Trying again");
                else {
                    log.log(Level.SEVERE, "Tag " + tag.name() +
                            " could not be deleted because exception: " + e.getMessage());
                    throw new RuntimeException(); // Custom exception?
                    // Execution should continue even if tag cannot be deleted?
                }
            }
        }

        log.log(Level.INFO, "Successfully deleted tag " + tag.name());
    }


    private List<Commit> getBranchCommitList(String branchName) throws IOException, GitAPIException {
        List<Commit> commits = new ArrayList<>();

        Iterable<RevCommit> rawCommits = git.log().add(repo.resolve(branchName)).call();

        for (RevCommit c : rawCommits)
            commits.add(parseRevCommit(c));

        return commits;
    }

    private Commit getTagCommit(Ref tag) throws IOException, GitAPIException {
        LogCommand logCmd = git.log();

        Ref peeledRef = repo.getRefDatabase().peel(tag);
        if (peeledRef.getPeeledObjectId() != null)
            logCmd.add(peeledRef.getPeeledObjectId());
        else
            logCmd.add(tag.getObjectId());

        Iterable<RevCommit> tagLog = logCmd.call();

        return parseRevCommit(tagLog.iterator().next());
    }

    private Commit parseRevCommit(RevCommit rawCommit) {
        String commitId = rawCommit.getId().getName();

        int commitTime = rawCommit.getCommitTime();

        PersonIdent rawAuthor = rawCommit.getAuthorIdent();
        CommitAuthor author = new CommitAuthor(rawAuthor.getName(), rawAuthor.getEmailAddress());

        return new Commit(commitId, commitTime, author);
    }
}
