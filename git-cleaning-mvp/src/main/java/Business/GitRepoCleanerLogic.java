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
                LOGGER.logRepoError(
                        String.format("Failed to clone repo due to '%s'. Skipping cleaning repo",
                        e.getMessage()), repoNum);
            } catch (GitUpdateException e) {
                LOGGER.logRepoError(
                        String.format("Failed to update local repo due to '%s'. Skipping cleaning repo",
                        e.getMessage()), repoNum);
            } catch (GitStartupException e) {
                LOGGER.logRepoError(
                        String.format("Failed to access local repo due to '%s'. Skipping cleaning repo",
                        e.getMessage()), repoNum);
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
        List<Branch> deletedBranches;
        List<Tag> deletedTags;
        try {
            List<Branch> branches = GIT.getBranches();
            List<String> branchNames = branches.stream().map(Branch::name).toList();
            LOGGER.logRepoMsg(String.format("Fetching branch list for repo: %s", branchNames), repoNum);

            List<Tag> tags = GIT.getTags();
            List<String> tagNames = tags.stream().map(Tag::name).toList();
            LOGGER.logRepoMsg(String.format("Fetching tag list for repo: %s", tagNames), repoNum);

            LOGGER.logRepoMsg("Begin processing branches", repoNum);
            deletedBranches = cleanBranches(branches, repoCleaningInfo.repoId(),
                    repoCleaningInfo.excludedBranches(), repoCleaningInfo.takeActionCountsDays(), repoNum);

            LOGGER.logRepoMsg("Begin processing tags", repoNum);
            deletedTags = cleanTags(tags, repoCleaningInfo.repoId(), repoCleaningInfo.takeActionCountsDays(),
                    repoNum);

        } catch (GitBranchFetchException | GitTagFetchException e) {
            LOGGER.logRepoError(
                    String.format("Error: '%s'. Skipping cleaning repo",
                    e.getMessage()), repoNum);
            return;
        }

        try {
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

        } catch (GitPushBranchDeletionException | GitPushNewTagsException | GitPushTagDeletionException e) {
            LOGGER.logRepoError(
                    String.format("Error: '%s'. Cannot push changes to remote",
                    e.getMessage()), repoNum);
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
                    LOGGER.logBranchWarn(
                            String.format("Failed to notify of pending archival of branch '%s'",
                            branch.name()), repoNum, branchNum);
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
            LOGGER.logBranchWarn(
                    String.format("Error: '%s'. Branch '%s' not archived",
                    e.getMessage(), branch.name()), repoNum, branchNum);

        } catch (GitBranchDeletionException e) {
            LOGGER.logBranchWarn(
                    String.format("Error: '%s'. Cannot archive branch",
                    e.getMessage()), repoNum, branchNum);
            try {
                GIT.deleteTag(newArchiveTag);

            } catch (GitTagDeletionException ex) {
                LOGGER.logBranchWarn(
                        String.format("Error: '%s'. Archive tag '%s' is extra",
                        ex.getMessage(), newArchiveTag.name()), repoNum, branchNum);
            }

        } catch (SendEmailException e) {
            LOGGER.logBranchWarn(
                    String.format("Failed to notify of archival of branch '%s'",
                    branch.name()), repoNum, branchNum);
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
                    LOGGER.logTagWarn(
                            String.format("Failed to notify of pending deletion of tag '%s'",
                            tag.name()), repoNum, tagNum);
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
            LOGGER.logTagWarn(
                    String.format("Error: '%s'. Archive tag not deleted",
                    e.getMessage()), repoNum, tagNum);
        } catch (SendEmailException e) {
            LOGGER.logTagWarn(
                    String.format("Failed to notify of deletion of tag '%s'",
                    tag.name()), repoNum, tagNum);
        }
    }

    private String formatCommitDate(int commitDate) {
        ZonedDateTime date = ZonedDateTime.ofInstant(Instant.ofEpochSecond(commitDate), ZoneOffset.UTC);

        DateTimeFormatter format = DateTimeFormatter.ofPattern("dd/MM/yy");

        return format.format(date);
    }
}
