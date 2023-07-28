import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

public class GitCleaner {

    private final Config CONFIG;
    private final ILogWrapper LOGGER;

    // Hardcoded for testing. Will pull from current time in final implementation
    private static final int executionTime = 1685682000;


    public GitCleaner(Config config, ILogWrapper logger, IGitCloner cloner) {
        this.CONFIG = config;
        this.LOGGER = logger;

        LOGGER.log(Level.INFO, "Checking for local repo");
        if (!localRepoExist(CONFIG.REPO_DIR)) {
            LOGGER.log(Level.INFO, "Repo does not exist locally, cloning from remote");

            if (cloner.cloneRepo(LOGGER))
                LOGGER.log(Level.INFO, "Repo successfully cloned from remote");
        }
    }


    public void clean(IGitWrapper git, IEmailHandler email) {
        LOGGER.log(Level.INFO, "Starting up git and updating from remote");
        if (git.startGit(LOGGER))
            LOGGER.log(Level.INFO, "Git successfully started up");
        else
            return;

        if (git.updateRepo(LOGGER))
            LOGGER.log(Level.INFO, "Repo successfully updated from remote");
        else
            return;


        LOGGER.log(Level.INFO, "Starting cleaning process");
        List<Branch> branches = git.getBranches(LOGGER);
        if (branches != null)
            LOGGER.log(Level.INFO, "Branches successfully obtained");
        else
            return;

        List<Tag> tags = git.getTags(LOGGER);
        if (tags != null)
            LOGGER.log(Level.INFO, "Tags successfully obtained");
        else
            return;

        List<Branch> deletedBranches = checkBranches(git, email, branches);
        List<Tag> deletedTags = checkTags(git, email, tags);
        LOGGER.log(Level.INFO, "Finished cleaning");


        LOGGER.log(Level.INFO, "Pushing updates to remote repo");
        for (Branch b : deletedBranches)
            git.pushDeletedBranch(b, LOGGER);

        git.pushNewTags(LOGGER);

        for (Tag t : deletedTags)
            git.pushDeletedTag(t, LOGGER);
        LOGGER.log(Level.INFO, "Updates successfully pushed to remote repo");
    }


    private boolean localRepoExist(String repoDirectory) {
        Path gitFolderPath = Paths.get(repoDirectory);
        Path directoryPath = Paths.get(repoDirectory.substring(0, repoDirectory.length() - 5));

        return Files.exists(directoryPath) && Files.exists(gitFolderPath);
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


    private List<Branch> checkBranches(IGitWrapper git, IEmailHandler email, List<Branch> branches) {
        List<Branch> deletedBranches = new ArrayList<>();
        LOGGER.log(Level.INFO, "Checking branches");

        for (Branch b : branches) {
            LOGGER.log(Level.INFO, "Checking branch " + b.name());

            if (CONFIG.EXCLUDED_BRANCHES.contains(b.name()))
                continue;

            int commitTime = b.commits().get(0).commitTime();
            int daysSinceCommit = (executionTime - commitTime) / 86400;

            if (daysSinceCommit == CONFIG.N - CONFIG.K) {
                notifyPendingArchival(email, b);

            } else if (daysSinceCommit >= CONFIG.N) {
                Tag newArchiveTag = buildArchiveTag(b);
                LOGGER.log(Level.INFO, String.format("Archiving branch %s as %s", b.name(), newArchiveTag.name()));

                boolean tagCreated = git.setTag(newArchiveTag, LOGGER);
                if (tagCreated) {
                    if (git.deleteBranch(b, LOGGER)) {
                        deletedBranches.add(b);
                        LOGGER.log(Level.INFO,
                                String.format("Branch %s successfully archived as %s",
                                b.name(), newArchiveTag.name()));

                        notifyArchival(email, b, newArchiveTag);

                    } else {
                        git.deleteTag(newArchiveTag, LOGGER);  // May have to account for this failing
                        LOGGER.log(Level.WARNING,
                                String.format("Failed to delete branch %s, archive tag %s removed",
                                b.name(), newArchiveTag.name()));
                    }
                } else
                    LOGGER.log(Level.WARNING,
                            String.format("Branch %s not archived due to error in tag process", b.name()));
            }
        }
        LOGGER.log(Level.INFO, "Finished checking branches");
        return deletedBranches;
    }

    private void notifyPendingArchival(IEmailHandler email, Branch b) {
        Email message = email.buildPendingArchivalEmail(b);

        if (email.sendEmail(message))
            LOGGER.log(Level.INFO,
                    String.format("Notification of pending archival for branch %s sent to %s",
                    b.name(), message.to().email()));
        else
            LOGGER.log(Level.WARNING,
                    String.format("Unable to send notification of pending archival for branch %s to %s",
                    b.name(), message.to().email()));
    }

    private void notifyArchival(IEmailHandler email, Branch b, Tag newArchiveTag) {
        Email message = email.buildArchivalEmail(b, newArchiveTag);

        if (email.sendEmail(message))
            LOGGER.log(Level.INFO,
                    String.format("Notification of archival of branch %s sent to %s",
                    b.name(),message.to().email()));
        else
            LOGGER.log(Level.WARNING,
                    String.format("Unable to send notification of archival of branch %s to %s",
                    b.name(), message.to().email()));
    }


    private List<Tag> checkTags(IGitWrapper git, IEmailHandler email, List<Tag> tags) {
        List<Tag> deletedTags = new ArrayList<>();
        LOGGER.log(Level.INFO, "Checking tags");

        for (Tag t : tags) {
            LOGGER.log(Level.INFO, "Checking tag " + t.name());

            if (!t.name().matches("zArchiveBranch_\\d{8}_[\\w-]+"))
                continue;

            int commitTime = t.commit().commitTime();
            int daysSinceCommit = (executionTime - commitTime) / 86400;

            if (daysSinceCommit == CONFIG.N + CONFIG.M - CONFIG.K) {
                notifyPendingTagDeletion(email, t);

            } else if (daysSinceCommit >= CONFIG.N + CONFIG.M) {
                if (git.deleteTag(t, LOGGER)) {
                    deletedTags.add(t);
                    LOGGER.log(Level.INFO,
                            String.format("Successfully deleted archive tag %s", t.name()));

                    notifyTagDeletion(email, t);
                } else
                    LOGGER.log(Level.WARNING,
                            String.format("Unable to delete archive tag %s", t.name()));
            }
        }
        LOGGER.log(Level.INFO, "Finished checking tags");
        return deletedTags;
    }

    private void notifyPendingTagDeletion(IEmailHandler email, Tag t) {
        Email message = email.buildPendingTagDeletionEmail(t);

        if (email.sendEmail(message))
            LOGGER.log(Level.INFO,
                    String.format("Notification of pending deletion of tag %s sent to %s",
                    t.name(), message.to().email()));
        else
            LOGGER.log(Level.WARNING,
                    String.format("Unable to send notification of pending tag deletion %s to %s",
                    t.name(), message.to().email()));
    }

    private void notifyTagDeletion(IEmailHandler email, Tag t) {
        Email message = email.buildTagDeletionEmail(t);

        if (email.sendEmail(message))
            LOGGER.log(Level.INFO,
                    String.format("Notification of deletion of tag %s sent to %s",
                    t.name(), message.to().email()));
        else
            LOGGER.log(Level.WARNING,
                    String.format("Unable to send notification of deletion of tag %s to %s",
                    t.name(), message.to().email()));
    }
}
