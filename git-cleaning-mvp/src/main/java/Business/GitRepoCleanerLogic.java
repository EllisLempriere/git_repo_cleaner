package Business;

import Application.ICustomLogger;
import Business.Models.*;
import Provider.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

public class GitRepoCleanerLogic implements IGitRepoCleanerLogic {

    private final List<RepoCleaningInfo> REPOS;
    private final IGitProvider GIT;
    private final INotificationLogic NOTIFICATION_LOGIC;
    private final ICustomLogger LOGGER;
    private final int EXECUTION_TIME;


    public GitRepoCleanerLogic(List<RepoCleaningInfo> repoCleaningInfoList, IGitProvider git,
                               INotificationLogic notificationLogic, ICustomLogger logger, int executionTime) {
        if (repoCleaningInfoList == null)
            throw new IllegalArgumentException("Repo cleaning list cannot be null");
        if (git == null)
            throw new IllegalArgumentException("Git provider cannot be null");
        if (notificationLogic == null)
            throw new IllegalArgumentException("Notification logic cannot be null");
        if (logger == null)
            throw new IllegalArgumentException("Logger cannot be null");
        if (executionTime < 0)
            throw new IllegalArgumentException("Execution time must be >= 0");

        this.REPOS = repoCleaningInfoList;
        this.GIT = git;
        this.NOTIFICATION_LOGIC = notificationLogic;
        this.LOGGER = logger;
        this.EXECUTION_TIME = executionTime;
    }


    @Override
    public void cleanRepos() {
        int repoNum = 1;
        for (RepoCleaningInfo repo : REPOS) {
            LOGGER.logRepoMsg(String.format("START REPO: %s", repo.remoteUri()), repoNum);

            try {
                selectRepo(repo.repoDir(), repo.remoteUri(), repoNum);

                cleanRepo(repo, repoNum);

                repoNum++;

            } catch (GitCloningException e) {
                LOGGER.log(Level.SEVERE, e.getMessage());
                LOGGER.log(Level.SEVERE, "Halting execution due to failed remote repo clone");
            } catch (GitUpdateException e) {
                LOGGER.log(Level.SEVERE, e.getMessage());
                LOGGER.log(Level.SEVERE, "Halting execution due to failed local repo update");
            } catch (GitStartupException e) {
                LOGGER.log(Level.SEVERE, e.getMessage());
                LOGGER.log(Level.SEVERE, "Halting execution due to failed initialization of git object");
            }
        }
    }

    @Override
    public void selectRepo(String repoDir, String remoteUri, int repoNum)
            throws GitCloningException, GitUpdateException, GitStartupException {
        if (!localRepoExist(repoDir)) {
            LOGGER.logRepoMsg(String.format("Local repo does not exist, cloning from remote to '%s'", repoDir), repoNum);
            GIT.cloneRepo(repoDir, remoteUri);
        } else
            LOGGER.logRepoMsg(String.format("Local repo exists at '%s', updating from remote", repoDir), repoNum);

        GIT.setupRepo(repoDir);
        GIT.updateRepo(repoDir);
    }

    @Override
    public void cleanRepo(RepoCleaningInfo repoCleaningInfo, int repoNum) {
        try {
            List<Branch> branches = GIT.getBranches();
            List<String> branchNames = new ArrayList<>();
            for (Branch b : branches)
                branchNames.add(b.name());
            LOGGER.logRepoMsg(String.format("Fetching branch list for repo: %s", branchNames), repoNum);

            List<Tag> tags = GIT.getTags();
            List<String> tagNames = new ArrayList<>();
            for (Tag t : tags)
                tagNames.add(t.name());
            LOGGER.logRepoMsg(String.format("Fetching tag list for repo: %s", tagNames), repoNum);

            LOGGER.logRepoMsg("Begin processing branches", repoNum);
            List<Branch> deletedBranches = cleanBranches(branches, repoCleaningInfo.repoId(),
                    repoCleaningInfo.excludedBranches(), repoCleaningInfo.takeActionCountsDays(), repoNum);

            LOGGER.logRepoMsg("Begin processing tags", repoNum);
            List<Tag> deletedTags = cleanTags(tags, repoCleaningInfo.repoId(), repoCleaningInfo.takeActionCountsDays(),
                    repoNum);

            List<String> deletedBranchNames = new ArrayList<>();
            for (Branch b : deletedBranches) {
                GIT.pushDeleteRemoteBranch(b);
                deletedBranchNames.add(b.name());
            }
            LOGGER.logRepoMsg(
                    String.format("Pushing %d stale branch removal(s) to remote: %s",
                    deletedBranches.size(), deletedBranchNames), repoNum);

            List<String> newArchiveTags = new ArrayList<>();
            for (Branch b : deletedBranches)
                newArchiveTags.add(new ArchiveTagName(EXECUTION_TIME, b.name()).name);
            GIT.pushNewTags();
            LOGGER.logRepoMsg(
                    String.format("Pushing %d newly created archive tag(s) to remote: %s",
                    deletedBranches.size(), newArchiveTags), repoNum);

            List<String> deletedTagNames = new ArrayList<>();
            for (Tag t : deletedTags) {
                GIT.pushDeleteRemoteTag(t);
                deletedTagNames.add(t.name());
            }
            LOGGER.logRepoMsg(
                    String.format("Pushing %d stale archive tag deletion(s) to remote: %s",
                    deletedTags.size(), deletedTagNames), repoNum);

        } catch (GitBranchFetchException e) {
            LOGGER.log(Level.SEVERE, e.getMessage());
            LOGGER.log(Level.SEVERE, "Halting execution due to failure to fetch branch list");
        } catch (GitTagFetchException e) {
            LOGGER.log(Level.SEVERE, e.getMessage());
            LOGGER.log(Level.SEVERE, "Halting execution due to failure to fetch tag list");
        } catch (GitPushBranchDeletionException e) {
            LOGGER.log(Level.SEVERE, e.getMessage());
            LOGGER.log(Level.SEVERE, "Halting execution due to failure to push deleted branch to remote");
        } catch (GitPushNewTagsException e) {
            LOGGER.log(Level.SEVERE, e.getMessage());
            LOGGER.log(Level.SEVERE, "Halting execution due to failure to push new tags to remote");
        } catch (GitPushTagDeletionException e) {
            LOGGER.log(Level.SEVERE, e.getMessage());
            LOGGER.log(Level.SEVERE, "Halting execution due to failure to push deletion of tag to remote");
        }
    }


    private static boolean localRepoExist(String repoDirectory) {
        Path gitFolderPath = Paths.get(repoDirectory + "\\.git");
        Path directoryPath = Paths.get(repoDirectory);

        return Files.exists(directoryPath) && Files.exists(gitFolderPath);
    }


    // Exposed for testing
    public List<Branch> cleanBranches(List<Branch> branches, String repoId, List<String> excludedBranches,
                                       TakeActionCountsDays takeActionCountsDays, int repoNum) {
        if (branches.isEmpty()) {
            LOGGER.logRepoMsg("No branches to process", repoNum);
            return new ArrayList<>();
        }

        List<Branch> deletedBranches = new ArrayList<>();

        int branchNum = 0;
        for (Branch branch : branches) {
            branchNum++;
            if (excludedBranches.contains(branch.name())) {
                LOGGER.logBranchMsg(
                        String.format("Inspecting branch '%s'. Branch is in excluded branch list", branch.name()),
                        repoNum, branchNum);
                LOGGER.logBranchMsg("Skipping excluded branch", repoNum, branchNum);
                continue;
            }

            int commitTime = branch.commits().get(0).commitTime();
            int daysSinceCommit = (EXECUTION_TIME - commitTime) / 86400;

            if (daysSinceCommit ==
                    takeActionCountsDays.staleBranchInactivityDays() - takeActionCountsDays.notificationBeforeActionDays()) {
                LOGGER.logBranchMsg(
                        String.format("Inspecting branch '%s'. Last commit on %s, %d day(s) ago",
                        branch.name(), formatCommitDate(commitTime), daysSinceCommit), repoNum, branchNum);
                LOGGER.logBranchMsg("Notifying developer(s) of pending branch archival", repoNum, branchNum);
                try {
                    NOTIFICATION_LOGIC.sendNotificationPendingArchival(branch, repoId, repoNum, branchNum);

                } catch (SendEmailException e) {
                    LOGGER.log(Level.WARNING,
                            String.format("Failed to notify of pending archival of branch '%s'",
                            branch.name()));
                }

            } else if (daysSinceCommit >= takeActionCountsDays.staleBranchInactivityDays()) {
                LOGGER.logBranchMsg(
                        String.format("Inspecting branch '%s'. Last commit on %s, %d day(s) ago",
                        branch.name(), formatCommitDate(commitTime), daysSinceCommit), repoNum, branchNum);

                archiveBranch(branch, repoId, repoNum, branchNum);
                deletedBranches.add(branch);

            } else {
                LOGGER.logBranchMsg(
                        String.format("Inspecting branch '%s'. Last commit on %s, %d day(s) ago",
                                branch.name(), formatCommitDate(commitTime), daysSinceCommit), repoNum, branchNum);
                LOGGER.logBranchMsg("Nothing to do", repoNum, branchNum);
            }
        }
        return deletedBranches;
    }


    // Exposed for testing
    public void archiveBranch(Branch branch, String repoId, int repoNum, int branchNum) {
        ArchiveTagName archiveTagName = new ArchiveTagName(EXECUTION_TIME, branch.name());
        Tag newArchiveTag = new Tag(archiveTagName.name, branch.commits());

        LOGGER.logBranchMsg(String.format("Archiving branch as tag '%s'", newArchiveTag.name()), repoNum, branchNum);

        try {
            GIT.createTag(newArchiveTag);
            GIT.deleteBranch(branch);

            NOTIFICATION_LOGIC.sendNotificationArchival(branch, newArchiveTag, repoId, repoNum, branchNum);

        } catch (GitCreateTagException e) {
            LOGGER.log(Level.WARNING,
                    String.format("Failed to create archive tag '%s', branch '%s' not archived",
                    newArchiveTag.name(), branch.name()));
        } catch (GitBranchDeletionException e) {
            LOGGER.log(Level.WARNING,
                    String.format("Failed to delete stale branch '%s', branch not archived, removing archive tag '%s'",
                    branch.name(), newArchiveTag.name()));
            try {
                GIT.deleteTag(newArchiveTag);
                LOGGER.log(Level.WARNING,
                        String.format("Archive tag '%s' successfully removed", newArchiveTag.name()));
            } catch (GitTagDeletionException ex) {
                LOGGER.log(Level.WARNING,
                        String.format("Failed to delete archive tag '%s', tag is extraneous", newArchiveTag.name()));
            }
        } catch (SendEmailException e) {
            LOGGER.log(Level.WARNING,
                    String.format("Failed to notify of archival of branch '%s'",
                    branch.name()));
        }
    }


    // Exposed for testing
    public List<Tag> cleanTags(List<Tag> tags, String repoId, TakeActionCountsDays takeActionCountsDays, int repoNum) {
        if (tags.isEmpty()) {
            LOGGER.logRepoMsg("No tags to process", repoNum);
            return new ArrayList<>();
        }

        List<Tag> deletedTags = new ArrayList<>();

        int tagNum = 0;
        for (Tag tag : tags) {
            tagNum++;
            ArchiveTagName archiveTag;
            if ((archiveTag = ArchiveTagName.tryParse(tag.name())) == null) {
                LOGGER.logTagMsg(
                        String.format("Inspecting tag '%s'. Tag is not an archive tag", tag.name()), repoNum, tagNum);
                LOGGER.logTagMsg("Skipping non archive tag", repoNum, tagNum);
                continue;
            }

            int tagCreationTime = (int) archiveTag.createDate.toInstant().getEpochSecond();
            int daysSinceCreation = (EXECUTION_TIME - tagCreationTime) / 86400;

            if (daysSinceCreation == takeActionCountsDays.staleTagDays() - takeActionCountsDays.notificationBeforeActionDays()) {
                LOGGER.logTagMsg(
                        String.format("Inspecting tag '%s'. Tag is %s day(s) old",
                        tag.name(), daysSinceCreation), repoNum, tagNum);
                LOGGER.logTagMsg("Notifying developer(s) of pending archive tag deletion", repoNum, tagNum);
                try {
                    NOTIFICATION_LOGIC.sendNotificationPendingTagDeletion(tag, repoId, repoNum, tagNum);

                } catch (SendEmailException e) {
                    LOGGER.log(Level.WARNING,
                            String.format("Failed to notify of pending deletion of archive tag '%s'",
                            tag.name()));
                }

            } else if (daysSinceCreation >= takeActionCountsDays.staleTagDays()) {
                LOGGER.logTagMsg(
                        String.format("Inspecting tag '%s'. Tag is %d day(s) old",
                        tag.name(), daysSinceCreation), repoNum, tagNum);
                LOGGER.logTagMsg("Deleting stale archive tag", repoNum, tagNum);

                deleteArchiveTag(tag, repoId, repoNum, tagNum);
                deletedTags.add(tag);

            } else {
                LOGGER.logTagMsg(
                        String.format("Inspecting tag '%s'. Tag is %d days(s) old",
                        tag.name(), daysSinceCreation), repoNum, tagNum);
            }
        }
        return deletedTags;
    }

    // Exposed for testing
    public void deleteArchiveTag(Tag tag, String repoId, int repoNum, int tagNum) {
        try {
            GIT.deleteTag(tag);

            NOTIFICATION_LOGIC.sendNotificationTagDeletion(tag, repoId, repoNum, tagNum);

        } catch (GitTagDeletionException e) {
            LOGGER.log(Level.WARNING,
                    String.format("Unable to delete archive tag '%s'", tag.name()));
        } catch (SendEmailException e) {
            LOGGER.log(Level.WARNING,
                    String.format("Failed to notify of deletion of archive tag '%s'",
                    tag.name()));
        }
    }

    private String formatCommitDate(int commitDate) {
        ZonedDateTime date = ZonedDateTime.ofInstant(Instant.ofEpochSecond(commitDate), ZoneOffset.UTC);

        DateTimeFormatter format = DateTimeFormatter.ofPattern("dd/MM/yy");

        return format.format(date);
    }
}
