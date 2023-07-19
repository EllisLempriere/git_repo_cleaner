import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.LogCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
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
import java.util.ArrayList;
import java.util.List;

// This class will assume the repo exists locally. Another class will take care setting it up if needed
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
}
