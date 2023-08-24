package Business;

import Application.ICustomLogger;
import Business.Models.*;
import Provider.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
        for (RepoCleaningInfo repo : REPOS) {
            LOGGER.log(Level.INFO, "Beginning cleaning repo with remote: " + repo.remoteUri());

            try {
                LOGGER.log(Level.INFO, "Setting up local repo at: " + repo.repoDir());
                selectRepo(repo.repoDir(), repo.remoteUri());
                LOGGER.log(Level.INFO, "Local repo set up");

                LOGGER.log(Level.INFO, "Starting cleaning");
                cleanRepo(repo);
                LOGGER.log(Level.INFO, "Successfully finished cleaning repo");

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
    public void selectRepo(String repoDir, String remoteUri)
            throws GitCloningException, GitUpdateException, GitStartupException {
        LOGGER.log(Level.INFO, "Checking if local repo exists");
        if (!localRepoExist(repoDir)) {
            LOGGER.log(Level.INFO, "Local repo does not exist, cloning from remote");
            GIT.cloneRepo(repoDir, remoteUri);

            LOGGER.log(Level.INFO,
                    String.format("Repo successfully cloned to local at %s", repoDir));
        } else
            LOGGER.log(Level.INFO, "Local repo exists, continuing");

        LOGGER.log(Level.INFO, "Prepping local repo");
        GIT.setupRepo(repoDir);
        LOGGER.log(Level.INFO, "Successfully prepped local repo");

        LOGGER.log(Level.INFO, "Updating local repo");
        GIT.updateRepo(repoDir);
        LOGGER.log(Level.INFO, "Repo successfully updated from remote");
    }

    @Override
    public void cleanRepo(RepoCleaningInfo repoCleaningInfo) {
        try {
            LOGGER.log(Level.INFO, "Getting branch list");
            List<Branch> branches = GIT.getBranches();
            LOGGER.log(Level.INFO, "Branch list successfully obtained");

            LOGGER.log(Level.INFO, "Getting tag list");
            List<Tag> tags = GIT.getTags();
            LOGGER.log(Level.INFO, "Tag list successfully obtained");

            LOGGER.log(Level.INFO, "Cleaning branches");
            List<Branch> deletedBranches = cleanBranches(branches, repoCleaningInfo.repoId(),
                    repoCleaningInfo.excludedBranches(), repoCleaningInfo.takeActionCountsDays());
            LOGGER.log(Level.INFO, "Finished cleaning branches");

            LOGGER.log(Level.INFO, "Cleaning tags");
            List<Tag> deletedTags = cleanTags(tags, repoCleaningInfo.repoId(), repoCleaningInfo.takeActionCountsDays());
            LOGGER.log(Level.INFO, "Finished cleaning tags");

            LOGGER.log(Level.INFO, "Finished cleaning");
            LOGGER.log(Level.INFO, "Beginning updating remote");

            LOGGER.log(Level.INFO, "Pushing branch deletions to remote");
            for (Branch b : deletedBranches) {
                LOGGER.log(Level.INFO, "Removing stale branch " + b.name() + "from remote");
                GIT.pushDeleteRemoteBranch(b);
                LOGGER.log(Level.INFO, "Successfully removed stale branch " + b.name() + " from remote");
            }
            LOGGER.log(Level.INFO, "Successfully pushed branch deletions to remote");

            LOGGER.log(Level.INFO, "Pushing newly created tags to remote");
            GIT.pushNewTags();
            LOGGER.log(Level.INFO, "Successfully pushed new tags to remote");

            LOGGER.log(Level.INFO, "Pushing tag deletions to remote");
            for (Tag t : deletedTags) {
                LOGGER.log(Level.INFO, "Removing archive tag " + t.name() + " from remote");
                GIT.pushDeleteRemoteTag(t);
            }
            LOGGER.log(Level.INFO, "Successfully pushed tag deletions to remote");

            LOGGER.log(Level.INFO, "All changes pushed to remote");

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
                                       TakeActionCountsDays takeActionCountsDays) {
        if (branches.isEmpty()) {
            LOGGER.log(Level.INFO, "No branches to clean");
            return new ArrayList<>();
        }

        List<Branch> deletedBranches = new ArrayList<>();

        for (Branch branch : branches) {
            LOGGER.log(Level.INFO, "Checking branch " + branch.name());

            if (excludedBranches.contains(branch.name())) {
                LOGGER.log(Level.INFO, "Branch " + branch.name() + " is one of excluded branches, skipping");
                continue;
            }

            int commitTime = branch.commits().get(0).commitTime();
            int daysSinceCommit = (EXECUTION_TIME - commitTime) / 86400;

            if (daysSinceCommit ==
                    takeActionCountsDays.staleBranchInactivityDays() - takeActionCountsDays.notificationBeforeActionDays()) {
                LOGGER.log(Level.INFO,
                        String.format("Has been %d days since last commit to branch '%s'. " +
                        "Notifying developer of pending archival", daysSinceCommit, branch.name()));

                try {
                    NOTIFICATION_LOGIC.sendNotificationPendingArchival(branch, repoId);
                    LOGGER.log(Level.INFO,
                            String.format("Notified pending archival of branch '%s'", branch.name()));

                } catch (SendEmailException e) {
                    LOGGER.log(Level.WARNING,
                            String.format("Failed to notify of pending archival of branch '%s'",
                            branch.name()));
                }

            } else if (daysSinceCommit >= takeActionCountsDays.staleBranchInactivityDays()) {
                LOGGER.log(Level.INFO,
                        String.format("Has been %d days since last commit to branch '%s'. " +
                        "Archiving branch", daysSinceCommit, branch.name()));

                archiveBranch(branch, repoId);
                deletedBranches.add(branch);

            } else {
                LOGGER.log(Level.INFO,
                        String.format("Branch '%s' is %d days old, nothing to do", branch.name(), daysSinceCommit));
            }
        }
        return deletedBranches;
    }


    // Exposed for testing
    public void archiveBranch(Branch branch, String repoId) {
        ArchiveTagName archiveTagName = new ArchiveTagName(EXECUTION_TIME, branch.name());
        Tag newArchiveTag = new Tag(archiveTagName.name, branch.commits());

        try {
            LOGGER.log(Level.INFO, "Creating new archive tag " + newArchiveTag.name());
            GIT.createTag(newArchiveTag);
            LOGGER.log(Level.INFO, "New archive tag " + newArchiveTag.name() + " successfully created");

            LOGGER.log(Level.INFO, "Deleting stale branch " + branch.name());
            GIT.deleteBranch(branch);
            LOGGER.log(Level.INFO, "Successfully deleted stale branch " + branch.name());

            LOGGER.log(Level.INFO,
                    String.format("Stale branch '%s' successfully archived as '%s'",
                    branch.name(), newArchiveTag.name()));

            NOTIFICATION_LOGIC.sendNotificationArchival(branch, newArchiveTag, repoId);
            LOGGER.log(Level.INFO,
                    String.format("Notified of archival of branch '%s'", branch.name()));

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
    public List<Tag> cleanTags(List<Tag> tags, String repoId, TakeActionCountsDays takeActionCountsDays) {
        if (tags.isEmpty()) {
            LOGGER.log(Level.INFO, "No tags to clean");
            return new ArrayList<>();
        }

        List<Tag> deletedTags = new ArrayList<>();

        for (Tag tag : tags) {
            LOGGER.log(Level.INFO, "Checking tag " + tag.name());

            ArchiveTagName archiveTag;
            if ((archiveTag = ArchiveTagName.tryParse(tag.name())) == null) {
                LOGGER.log(Level.INFO, "Tag " + tag.name() + " is not an archive tag, skipping");
                continue;
            }

            int tagCreationTime = (int) archiveTag.createDate.toInstant().getEpochSecond();
            int daysSinceCommit = (EXECUTION_TIME - tagCreationTime) / 86400;

            if (daysSinceCommit == takeActionCountsDays.staleTagDays() - takeActionCountsDays.notificationBeforeActionDays()) {
                LOGGER.log(Level.INFO,
                        String.format("Has been %d days since last commit on tag '%s'. " +
                        "Notifying developer of pending deletion", daysSinceCommit, tag.name()));

                try {
                    NOTIFICATION_LOGIC.sendNotificationPendingTagDeletion(tag, repoId);
                    LOGGER.log(Level.INFO,
                            String.format("Notified of pending deletion of archive tag '%s'", tag.name()));
                } catch (SendEmailException e) {
                    LOGGER.log(Level.WARNING,
                            String.format("Failed to notify of pending deletion of archive tag '%s'",
                            tag.name()));
                }

            } else if (daysSinceCommit >= takeActionCountsDays.staleTagDays()) {
                LOGGER.log(Level.INFO,
                        String.format("Has been %d days since last commit to tag %s. " +
                        "Removing archive tag", daysSinceCommit, tag.name()));

                deleteArchiveTag(tag, repoId);
                deletedTags.add(tag);

            } else {
                LOGGER.log(Level.INFO,
                        String.format("Tag %s is %d days old, nothing to do", tag.name(), daysSinceCommit));
            }
        }
        return deletedTags;
    }

    // Exposed for testing
    public void deleteArchiveTag(Tag tag, String repoId) {
        try {
            LOGGER.log(Level.INFO, "Deleting archive tag '" + tag.name() + "'");
            GIT.deleteTag(tag);
            LOGGER.log(Level.INFO, "Successfully removed archive tag '" + tag.name() + "'");

            NOTIFICATION_LOGIC.sendNotificationTagDeletion(tag, repoId);
            LOGGER.log(Level.INFO,
                    String.format("Notified of deletion of archive tag '%s'", tag.name()));

        } catch (GitTagDeletionException e) {
            LOGGER.log(Level.WARNING,
                    String.format("Unable to delete archive tag '%s'", tag.name()));
        } catch (SendEmailException e) {
            LOGGER.log(Level.WARNING,
                    String.format("Failed to notify of deletion of archive tag '%s'",
                    tag.name()));
        }
    }
}
