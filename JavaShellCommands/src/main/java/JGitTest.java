import org.eclipse.jgit.api.*;
import org.eclipse.jgit.api.errors.*;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JGitTest {

    private static final String filePath = "C:\\Users\\ellis\\Documents\\repos\\test-case-5\\.git";
    private static final List<String> excludedBranches = List.of(new String[]{"main"});
    private static final long executionTime = 1685682000L; // 2023-06-01 22:00:00 in epoch sec
    private static final int n = 60;
    private static final int m = 30;
    private static final int k = 7;


    public static void main(String[] args) {
        Repository repo = openRepo(new File(filePath));
        Git git = startGit(repo);

        Map<String, Ref> branchesToCheck = getBranchesToCheck(git);
        for (Map.Entry<String, Ref> b : branchesToCheck.entrySet()) {
            String branchName = b.getKey();
            Ref branch = b.getValue();

            RevCommit mostRecentCommit = getNewestCommitOnBranch(git, repo, branchName);
            int daysSinceCommit = calculateDaysSinceCommit(mostRecentCommit);

            if (daysSinceCommit == n - k) {
                // Send notification of pending archival
            } else if (daysSinceCommit >= n) {
                // Send notification of archival
                archiveBranch(git, branchName);
            }
        }

        Map<String, Ref> tagsToCheck = getTagsToCheck(git);
        for (Map.Entry<String, Ref> t : tagsToCheck.entrySet()) {
            String tagName = t.getKey();
            Ref tag = t.getValue();

            RevCommit mostRecentCommit = getNewestCommitOnTag(git, repo, tag);
            int daysSinceCommit = calculateDaysSinceCommit(mostRecentCommit);

            if (daysSinceCommit == n + m - k) {
                // Send notification of pending archive tag deletion
            } else if (daysSinceCommit >= n + m) {
                // Send notification of archive tag deletion
                deleteTag(git, tagName);
            }
        }
    }


    private static Repository openRepo(File directory) {
        FileRepositoryBuilder repoBuilder = new FileRepositoryBuilder();
        try (Repository repo = repoBuilder.setGitDir(directory)
                .readEnvironment()
                .findGitDir()
                .build()) {

            return repo;

        } catch (IOException e) {
            throw new RuntimeException("Issue in opening repo: " + e.getMessage());
        }
    }

    private static Git startGit(Repository repo) {
        try (Git git = new Git(repo)) {
            return git;
        }
    }

    private static Map<String, Ref> getBranchesToCheck(Git git) {
        Map<String, Ref> branchesToCheck = new HashMap<>();

        try {
            List<Ref> branches = git.branchList().call();

            for (Ref b : branches) {
                String[] refPath = b.getName().split("/");
                String branchName = refPath[refPath.length - 1];

                if (!excludedBranches.contains(branchName))
                    branchesToCheck.put(branchName, b);
            }

            return branchesToCheck;

        } catch (GitAPIException e) {
            throw new RuntimeException("Issue in getting branch list: " + e.getMessage());
        }
    }

    private static RevCommit getNewestCommitOnBranch(Git git, Repository repo, String branchName) {
        try {
            Iterable<RevCommit> branchLog = git.log().add(repo.resolve(branchName)).call();
            return branchLog.iterator().next();

        } catch (GitAPIException | IOException e) {
            throw new RuntimeException("Issue finding newest commit on branch " + branchName + ": " + e.getMessage());
        }
    }

    private static int calculateDaysSinceCommit(RevCommit commit) {
        int mostRecentCommitTime = commit.getCommitTime();

        return (int) (executionTime - mostRecentCommitTime) / 86400;
    }

    private static void archiveBranch(Git git, String branchName) {
        try {
            git.tag().setName(createArchiveTagName(branchName)).call();

            git.checkout().setName("main").call(); // log an error, but don't halt execution
            git.branchDelete().setBranchNames(branchName).setForce(true).call();

        } catch (GitAPIException e) {
            throw new RuntimeException("Issue archiving branch " + branchName + ": " + e.getMessage());
        }
    }

    private static String createArchiveTagName(String branchName) {
        StringBuilder sb = new StringBuilder();

        sb.append("zArchiveBranch_");

        ZoneId zoneId = ZoneId.systemDefault();
        ZonedDateTime exeTime = ZonedDateTime.ofInstant(Instant.ofEpochSecond(executionTime), zoneId);
        String formattedTime = exeTime.format(DateTimeFormatter.BASIC_ISO_DATE).substring(0, 8);
        sb.append(formattedTime);

        sb.append("_");
        sb.append(branchName);

        return sb.toString();
    }

    private static Map<String, Ref> getTagsToCheck(Git git) {
        Map<String, Ref> tagsToCheck = new HashMap<>();

        try {
            List<Ref> tags = git.tagList().call();

            for (Ref t : tags) {
                String[] refPath = t.getName().split("/");
                String tagName = refPath[refPath.length - 1];

                if (tagName.matches("zArchiveBranch_\\d{8}_\\w*"))
                    tagsToCheck.put(tagName, t);
            }

            return tagsToCheck;

        } catch (GitAPIException e) {
            throw new RuntimeException("Issue getting tag list: " + e.getMessage());
        }
    }

    private static RevCommit getNewestCommitOnTag(Git git, Repository repo, Ref tag) {
        try {
            LogCommand logCmd = git.log();

            Ref peeledRef = repo.getRefDatabase().peel(tag);
            if (peeledRef.getPeeledObjectId() != null)
                logCmd.add(peeledRef.getPeeledObjectId());
            else
                logCmd.add(tag.getObjectId());

            Iterable<RevCommit> tagLog = logCmd.call();
            return tagLog.iterator().next();

        } catch (GitAPIException | IOException e) {
            throw new RuntimeException("Issue finding newest commit on tag " + tag.getName() + ": " + e.getMessage());
        }
    }

    private static void deleteTag(Git git, String tagName) {
        try {
            git.tagDelete().setTags(tagName).call();
        } catch (GitAPIException e) {
            throw new RuntimeException("Issue deleting tag" + tagName + ": " + e.getMessage());
        }
    }
}
