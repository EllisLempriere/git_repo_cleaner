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


    public GitCleaner(Config config, ILogWrapper logger) {
        this.CONFIG = config;
        this.LOGGER = logger;
    }


    public void clean() {
        LOGGER.log(Level.INFO, "Ensuring local repo is up-to-date");

        UserInfo user = new UserInfo("credentials.properties");
        GitRemoteHandler remote = new GitRemoteHandler(CONFIG.REPO_DIR, CONFIG.REMOTE_URI, user);
        Path gitPath = Paths.get(CONFIG.REPO_DIR);
        Path dirPath = Paths.get(CONFIG.REPO_DIR.substring(0, CONFIG.REPO_DIR.length() - 5));

        boolean updated = false;
        if (!Files.exists(dirPath) || !Files.exists(gitPath))
            updated = remote.cloneRepo(LOGGER);
        else if (Files.exists(dirPath) && Files.exists(gitPath))
            updated = remote.updateRepo(LOGGER);

        if (!updated) {
            LOGGER.log(Level.SEVERE, "Was not able to update local repo");
            return;
        }

        LOGGER.log(Level.INFO, "Local repo is up-to-date");

        LOGGER.log(Level.INFO, "Starting cleaning");

        GitWrapper git = new GitWrapper(CONFIG.REPO_DIR, LOGGER);
        IEmailSender sender = new EmailSender();

        List<Branch> branches = git.getBranches(LOGGER);
        List<Tag> tags = git.getTags(LOGGER);

        // Getting all branches and tags before doing any processing. This way if a branch is older than n+m days,
        // it will get archived on the first day and will not get deleted until another run the next day
        // Change this behavior?

        List<Branch> deletedBranches = new ArrayList<>();
        if (branches != null) {
            LOGGER.log(Level.INFO, "Checking branches");

            for (Branch b : branches) {
                LOGGER.log(Level.INFO, "Checking branch " + b.name());

                if (CONFIG.EXCLUDED_BRANCHES.contains(b.name()))
                    continue;

                int commitTime = b.commits().get(0).commitTime();
                int daysSinceCommit = (executionTime - commitTime) / 86400;

                if (daysSinceCommit == CONFIG.N - CONFIG.K) {
                    Email email = new Email(b.commits().get(0).author(),
                            String.format("Branch %s will be archived in %d days", b.name(), CONFIG.K));
                    if (sender.sendEmail(email))
                        LOGGER.log(Level.INFO,
                                String.format("Notification of pending archival for branch %s sent to %s",
                                b.name(), email.to().email()));

                } else if (daysSinceCommit >= CONFIG.N) {
                    Tag newArchiveTag = buildArchiveTag(b);

                    Email email = new Email(b.commits().get(0).author(),
                            String.format("Branch %s archived as %s", b.name(), newArchiveTag.name()));
                    if (sender.sendEmail(email))
                        LOGGER.log(Level.INFO,
                                String.format("Notification of archival of branch %s sent to %s",
                                b.name(), email.to().email()));

                    LOGGER.log(Level.INFO, "Archiving branch " + b.name() + " as " +  newArchiveTag.name());

                    boolean tagCreated = git.setTag(newArchiveTag, LOGGER);

                    if (tagCreated) {
                        git.deleteBranch(b, LOGGER);
                        deletedBranches.add(b);
                    }
                }
            }
        }
        LOGGER.log(Level.INFO, "Finished checking branches");

        List<Tag> deletedTags = new ArrayList<>();
        if (tags != null) {
            LOGGER.log(Level.INFO, "Checking tags");

            for (Tag t : tags) {
                LOGGER.log(Level.INFO, "Checking tag " + t.name());

                if (!t.name().matches("zArchiveBranch_\\d{8}_\\w*"))
                    continue;

                int commitTime = t.commit().commitTime();
                int daysSinceCommit = (executionTime - commitTime) / 86400;

                if (daysSinceCommit == CONFIG.N + CONFIG.M - CONFIG.K) {
                    Email email = new Email(t.commit().author(),
                            String.format("Archive tag %s will be deleted in %d days", t.name(), CONFIG.K));
                    if (sender.sendEmail(email))
                        LOGGER.log(Level.INFO,
                                String.format("Notification of pending archive tag deletion of tag %s sent to %s",
                                t.name(), email.to().email()));

                } else if (daysSinceCommit >= CONFIG.N + CONFIG.M) {
                    Email email = new Email(t.commit().author(),
                            String.format("Archive tag %s deleted", t.name()));
                    if (sender.sendEmail(email))
                        LOGGER.log(Level.INFO,
                                String.format("Notification of archive tag deletion of tag %s sent to %s",
                                t.name(), email.to().email()));

                    git.deleteTag(t, LOGGER);
                    deletedTags.add(t);
                }
            }
            LOGGER.log(Level.INFO, "Finished checking tags");
        }

        LOGGER.log(Level.INFO, "Successfully finished cleaning");

        LOGGER.log(Level.INFO, "Pushing updates to remote repo");

        if (!deletedBranches.isEmpty())
            if (remote.pushBranchDeletions(deletedBranches, LOGGER))
                LOGGER.log(Level.SEVERE, "Unable to push branch changes");

        if (!remote.pushNewTags(LOGGER))
            LOGGER.log(Level.SEVERE, "Unable to push new tags");

        if (!deletedTags.isEmpty())
            if (!remote.pushTagDeletions(deletedTags, LOGGER))
                LOGGER.log(Level.SEVERE, "Unable to push tag deletions to remote");

        LOGGER.log(Level.INFO, "Updates successfully pushed to remote repo");
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
}
