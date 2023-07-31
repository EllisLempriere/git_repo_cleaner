package Business;

import Application.ICustomLogger;
import Provider.*;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

public class GitCleaner {

    private final int DAYS_TO_STALE_BRANCH;
    private final int DAYS_TO_STALE_TAG;
    private final int PRECEDING_DAYS_TO_WARN;
    private final List<String> EXCLUDED_BRANCHES;
    private final IGitWrapper GIT;
    private final IEmailHandler MAIL;
    private final ICustomLogger LOGGER;

    // Hardcoded for testing. Will pull from current time in final implementation
    private static final int executionTime = 1685682000;


    public GitCleaner(int daysToStaleBranch, int daysToStaleTag, int precedingDaysToWarn,
                      List<String> excludedBranches, IGitWrapper git, IEmailHandler mail, ICustomLogger logger) {
        this.DAYS_TO_STALE_BRANCH = daysToStaleBranch;
        this.DAYS_TO_STALE_TAG = daysToStaleTag;
        this.PRECEDING_DAYS_TO_WARN = precedingDaysToWarn;
        this.EXCLUDED_BRANCHES = excludedBranches;
        this.GIT = git;
        this.MAIL = mail;
        this.LOGGER = logger;
    }


    public void clean() {
        try {
            LOGGER.log(Level.INFO, "Starting up git");
            GIT.startGit();
            LOGGER.log(Level.INFO, "Git successfully started up");

            LOGGER.log(Level.INFO, "Updating local repo");
            GIT.updateRepo();
            LOGGER.log(Level.INFO, "Repo successfully updated from remote");

            LOGGER.log(Level.INFO, "Getting branch list");
            List<Branch> branches = GIT.getBranches();
            LOGGER.log(Level.INFO, "Branch list successfully obtained");

            LOGGER.log(Level.INFO, "Getting tag list");
            List<Tag> tags = GIT.getTags();
            LOGGER.log(Level.INFO, "Tag list successfully obtained");

            LOGGER.log(Level.INFO, "Cleaning branches");
            List<Branch> deletedBranches = cleanBranches(branches);
            LOGGER.log(Level.INFO, "Finished cleaning branches");

            LOGGER.log(Level.INFO, "Cleaning tags");
            List<Tag> deletedTags = cleanTags(tags);
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

        } catch (GitStartupException e) {
            LOGGER.log(Level.SEVERE, e.getMessage());
            LOGGER.log(Level.SEVERE, "Halting execution due to failing git start up");
        } catch (GitUpdateException e) {
            LOGGER.log(Level.SEVERE, e.getMessage());
            LOGGER.log(Level.SEVERE, "Halting execution due to failing to update local repo");
        } catch (GitBranchFetchException e) { // This exception and down, maybe do not stop execution?
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

    private Tag buildArchiveTag(Branch branch) {
        StringBuilder tagName = new StringBuilder();

        tagName.append("zArchiveBranch_");

        ZonedDateTime exeTime = ZonedDateTime.ofInstant(Instant.ofEpochSecond(executionTime), ZoneId.systemDefault());
        String formattedTime = exeTime.format(DateTimeFormatter.BASIC_ISO_DATE).substring(0, 8);
        tagName.append(formattedTime);

        tagName.append("_");
        tagName.append(branch.name());

        return new Tag(tagName.toString(), branch.commits().get(0));
    }


    private List<Branch> cleanBranches(List<Branch> branches) {
        List<Branch> deletedBranches = new ArrayList<>();

        for (Branch branch : branches) {
            LOGGER.log(Level.INFO, "Checking branch " + branch.name());

            if (EXCLUDED_BRANCHES.contains(branch.name())) {
                LOGGER.log(Level.INFO, "Branch " + branch.name() + " is one of excluded branches, skipping");
                continue;
            }

            int commitTime = branch.commits().get(0).commitTime();
            int daysSinceCommit = (executionTime - commitTime) / 86400;

            if (daysSinceCommit == DAYS_TO_STALE_BRANCH - PRECEDING_DAYS_TO_WARN) {
                LOGGER.log(Level.INFO,
                        String.format("Has been %d days since last commit to branch %s. " +
                        "Notifying developer of pending archival", daysSinceCommit, branch.name()));

                notifyPendingArchival(branch);

            } else if (daysSinceCommit >= DAYS_TO_STALE_BRANCH) {
                LOGGER.log(Level.INFO,
                        String.format("Has been %d days since last commit to branch %s. " +
                        "Archiving branch", daysSinceCommit, branch.name()));

                archiveBranch(branch);
                deletedBranches.add(branch);

            } else {
                LOGGER.log(Level.INFO,
                        String.format("Branch %s is %d days old, nothing to do", branch.name(), daysSinceCommit));
            }
        }
        return deletedBranches;
    }

    private void notifyPendingArchival(Branch branch) {
        Email message = MAIL.buildPendingArchivalEmail(branch);

        try {
            MAIL.sendEmail(message);
            LOGGER.log(Level.INFO,
                    String.format("%s notified of pending archival of branch %s",
                    message.to().email(), branch.name()));

        } catch (SendEmailException e) {
            LOGGER.log(Level.WARNING,
                    String.format("Failed to notify %s of pending archival of branch %s because %s",
                    message.to().email(), branch.name(), e.getMessage()));
        }
    }

    private void archiveBranch(Branch branch) {
        Tag newArchiveTag = buildArchiveTag(branch);

        try {
            LOGGER.log(Level.INFO, "Creating new archive tag " + newArchiveTag.name());
            GIT.setTag(newArchiveTag);
            LOGGER.log(Level.INFO, "New archive tag " + newArchiveTag.name() + " successfully created");

            LOGGER.log(Level.INFO, "Deleting stale branch " + branch.name());
            GIT.deleteBranch(branch);
            LOGGER.log(Level.INFO, "Successfully deleted stale branch " + branch.name());

            notifyArchival(branch, newArchiveTag);

            LOGGER.log(Level.INFO,
                    String.format("Stale branch %s successfully archived as %s and notification sent",
                    branch.name(), newArchiveTag.name()));

        } catch (GitSetTagException e) {
            LOGGER.log(Level.WARNING,
                    String.format("Failed to create archive tag %s, branch %s not archived",
                    newArchiveTag.name(), branch.name()));
        } catch (GitBranchDeletionException e) {
            LOGGER.log(Level.WARNING,
                    String.format("Failed to delete stale branch %s, branch not archived, removing archive tag %s",
                    branch.name(), newArchiveTag.name()));
            try {
                GIT.deleteTag(newArchiveTag);
                LOGGER.log(Level.WARNING,
                        String.format("Archive tag %s successfully removed", newArchiveTag.name()));
            } catch (GitTagDeletionException ex) {
                LOGGER.log(Level.WARNING,
                        String.format("Failed to delete archive tag %s, tag is extraneous", newArchiveTag.name()));
            }
        }
    }

    private void notifyArchival(Branch branch, Tag tag) {
        Email message = MAIL.buildArchivalEmail(branch, tag);

        try {
            MAIL.sendEmail(message);
            LOGGER.log(Level.INFO,
                    String.format("%s notified of archival of branch %s",
                    message.to().email(), branch.name()));

        } catch (SendEmailException e) {
            LOGGER.log(Level.WARNING,
                    String.format("Failed to notify %s of archival of branch %s because %s",
                    message.to().email(), branch.name(), e.getMessage()));
        }
    }


    private List<Tag> cleanTags(List<Tag> tags) {
        List<Tag> deletedTags = new ArrayList<>();

        for (Tag tag : tags) {
            LOGGER.log(Level.INFO, "Checking tag " + tag.name());

            if (!tag.name().matches("zArchiveBranch_\\d{8}_[\\w-]+")) {
                LOGGER.log(Level.INFO, "Tag " + tag.name() + " is not an archive tag, skipping");
                continue;
            }

            int commitTime = tag.commit().commitTime();
            int daysSinceCommit = (executionTime - commitTime) / 86400;

            if (daysSinceCommit == DAYS_TO_STALE_BRANCH + DAYS_TO_STALE_TAG - PRECEDING_DAYS_TO_WARN) {
                LOGGER.log(Level.INFO,
                        String.format("Has been %d days since last commit on tag %s. " +
                        "Notifying developer of pending deletion", daysSinceCommit, tag.name()));

                notifyPendingTagDeletion(tag);

            } else if (daysSinceCommit >= DAYS_TO_STALE_BRANCH + DAYS_TO_STALE_TAG) {
                LOGGER.log(Level.INFO,
                        String.format("Has been %d days since last commit to tag %s. " +
                        "Removing archive tag", daysSinceCommit, tag.name()));

                deleteArchiveTag(tag);
                deletedTags.add(tag);

            } else {
                LOGGER.log(Level.INFO,
                        String.format("Tag %s is %d days old, nothing to do", tag.name(), daysSinceCommit));
            }
        }
        return deletedTags;
    }

    private void notifyPendingTagDeletion(Tag tag) {
        Email message = MAIL.buildPendingTagDeletionEmail(tag);

        try {
            MAIL.sendEmail(message);
            LOGGER.log(Level.INFO,
                    String.format("%s notified of pending deletion of archive tag %s",
                    message.to().email(), tag.name()));

        } catch (SendEmailException e) {
            LOGGER.log(Level.WARNING,
                    String.format("Failed to notify %s of pending deletion of archive tag %s because %s",
                    message.to().email(), tag.name(), e.getMessage()));
        }
    }

    private void deleteArchiveTag(Tag tag) {
        try {
            LOGGER.log(Level.INFO, "Deleting archive tag " + tag.name());
            GIT.deleteTag(tag);
            LOGGER.log(Level.INFO, "Successfully removed archive tag " + tag.name());

            notifyTagDeletion(tag);

            LOGGER.log(Level.INFO,
                    String.format("Archive tag %s successfully deleted and notification sent",
                    tag.name()));

        } catch (GitTagDeletionException e) {
            LOGGER.log(Level.WARNING,
                    String.format("Unable to delete archive tag %s because %s", tag.name(), e.getMessage()));
        }
    }

    private void notifyTagDeletion(Tag tag) {
        Email message = MAIL.buildTagDeletionEmail(tag);

        try {
            MAIL.sendEmail(message);
            LOGGER.log(Level.INFO,
                    String.format("%s notified of deletion of archive tag %s",
                    message.to().email(), tag.name()));

        } catch (SendEmailException e) {
            LOGGER.log(Level.WARNING,
                    String.format("Failed to notify %s of deletion of archive tag %s because %s",
                    message.to().email(), tag.name(), e.getMessage()));
        }
    }
}
