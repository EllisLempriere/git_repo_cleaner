import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.LogCommand;
import org.eclipse.jgit.api.errors.ConcurrentRefUpdateException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidTagNameException;
import org.eclipse.jgit.api.errors.NoHeadException;
import org.eclipse.jgit.errors.AmbiguousObjectException;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

// This class will assume the repo exists locally. Another class will take care setting it up if needed

// This class is starting to feel quite bloated. Not sure if I'm miss-calibrated or if it really is
// Should fetching data be one class and making changes be another?

// Needs a rename
public class GitCleaner implements IGitCleaner {

    private final Repository repo;
    private final Git git;


    public GitCleaner(String filePath) {
        File directory = new File(filePath);

        FileRepositoryBuilder repoBuilder = new FileRepositoryBuilder();
        try (Repository repo = repoBuilder.setGitDir(directory)
                .readEnvironment()
                .findGitDir()
                .build()) {

            this.repo = repo;

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        try (Git git = new Git(repo)) {
            this.git = git;
        }
    }


    @Override
    public List<Branch> getBranches() {
        List<Branch> branches = new ArrayList<>();

        try {
            List<Ref> rawBranches = git.branchList().call();

            for (Ref b : rawBranches) {
                String[] refPath = b.getName().split("/");
                String branchName = refPath[refPath.length - 1];

                List<Commit> commits = getBranchCommitList(branchName);

                branches.add(new Branch(branchName, commits));
            }

        } catch (GitAPIException e) {
            throw new RuntimeException(e);
        }

        return branches;
    }

    @Override
    public List<Tag> getTags() {
        List<Tag> tags = new ArrayList<>();

        try {
            List<Ref> rawTags = git.tagList().call();

            for (Ref t : rawTags) {
                String[] refPath = t.getName().split("/");
                String tagName = refPath[refPath.length - 1];

                Commit commit = getTagCommit(t);

                tags.add(new Tag(tagName, commit));
            }

        } catch (GitAPIException e) {
            throw new RuntimeException(e);
        }

        return tags;
    }

    // Should this have a confirmation return?
    @Override
    public void archiveBranch(Branch branch) {
        try {
            git.tag().setName(createArchiveTagName(branch.name())).call();

            // Should this cause an error from being checked out on the branch attempting to be deleted,
            // log an error but don't halt program execution
            git.branchDelete().setBranchNames(branch.name()).setForce(true).call();

        } catch (NoHeadException e) {
            throw new RuntimeException(e);
        } catch (InvalidTagNameException e) {
            throw new RuntimeException(e);
        } catch (ConcurrentRefUpdateException e) {
            throw new RuntimeException(e);
        } catch (GitAPIException e) {
            throw new RuntimeException(e);
        }
    }

    // Should this return something to confirm tag deletion?
    @Override
    public void deleteTag(Tag tag) {
        try {
            git.tagDelete().setTags(tag.name()).call();
        } catch (GitAPIException e) {
            throw new RuntimeException(e);
        }
    }


    private List<Commit> getBranchCommitList(String branch) {
        List<Commit> commits = new ArrayList<>();

        try {
            Iterable<RevCommit> rawCommits = git.log().add(repo.resolve(branch)).call();

            for (RevCommit c : rawCommits)
                commits.add(parseRevCommit(c));

        // Leaving exceptions alone for the time being until logging gets set up
        } catch (NoHeadException e) {
            throw new RuntimeException(e);
        } catch (GitAPIException e) {
            throw new RuntimeException(e);
        } catch (AmbiguousObjectException e) {
            throw new RuntimeException(e);
        } catch (IncorrectObjectTypeException e) {
            throw new RuntimeException(e);
        } catch (MissingObjectException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return commits;
    }

    private Commit getTagCommit(Ref tag) {
        try {
            LogCommand logCmd = git.log();

            Ref peeledRef = repo.getRefDatabase().peel(tag);
            if (peeledRef.getPeeledObjectId() != null)
                logCmd.add(peeledRef.getPeeledObjectId());
            else
                logCmd.add(tag.getObjectId());

            Iterable<RevCommit> tagLog = logCmd.call();

            return parseRevCommit(tagLog.iterator().next());

        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (NoHeadException e) {
            throw new RuntimeException(e);
        } catch (GitAPIException e) {
            throw new RuntimeException(e);
        }
    }

    private Commit parseRevCommit(RevCommit rawCommit) {
        String commitId = rawCommit.getId().getName();

        int commitTime = rawCommit.getCommitTime();

        PersonIdent rawAuthor = rawCommit.getAuthorIdent();
        CommitAuthor author = new CommitAuthor(rawAuthor.getName(), rawAuthor.getEmailAddress());

        return new Commit(commitId, commitTime, author);
    }

    private String createArchiveTagName(String branchName) {
        StringBuilder sb = new StringBuilder();

        sb.append("zArchiveBranch_");

        // Hardcoded time for testing. Will use current time in final implementation
        ZonedDateTime exeTime = ZonedDateTime.ofInstant(Instant.ofEpochSecond(1685682000), ZoneId.systemDefault());
        String formattedTime = exeTime.format(DateTimeFormatter.BASIC_ISO_DATE).substring(0, 8);
        sb.append(formattedTime);

        sb.append("_");
        sb.append(branchName);

        return sb.toString();
    }
}
