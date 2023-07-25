import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
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
        LOGGER.log(Level.INFO, "Starting cleaning");

        GitWrapper git = new GitWrapper(CONFIG.REPO_DIR, LOGGER);

        List<Branch> branches = git.getBranches(LOGGER);
        List<Tag> tags = git.getTags(LOGGER);

        // Getting all branches and tags before doing any processing. This way if a branch is older than n+m days,
        // it will get archived on the first day and will not get deleted until another run the next day
        // Change this behavior?

        if (branches != null) {
            LOGGER.log(Level.INFO, "Checking branches");

            for (Branch b : branches) {
                LOGGER.log(Level.INFO, "Checking branch " + b.name());

                if (CONFIG.EXCLUDED_BRANCHES.contains(b.name()))
                    continue;

                int commitTime = b.commits().get(0).commitTime();
                int daysSinceCommit = (executionTime - commitTime) / 86400;

                if (daysSinceCommit == CONFIG.N - CONFIG.K) {
                    // Send email for pending archival
                } else if (daysSinceCommit >= CONFIG.N) {
                    // Send email for archival
                    Tag newArchiveTag = buildArchiveTag(b);
                    LOGGER.log(Level.INFO, "Archiving branch " + b.name() + " as " +  newArchiveTag.name());

                    boolean tagCreated = git.setTag(newArchiveTag, LOGGER);

                    if (tagCreated)
                        git.deleteBranch(b, LOGGER);
                }
            }
        }
        LOGGER.log(Level.INFO, "Finished checking branches");

        if (tags != null) {
            LOGGER.log(Level.INFO, "Checking tags");

            for (Tag t : tags) {
                LOGGER.log(Level.INFO, "Checking tag " + t.name());

                if (!t.name().matches("zArchiveBranch_\\d{8}_\\w*"))
                    continue;

                int commitTime = t.commit().commitTime();
                int daysSinceCommit = (executionTime - commitTime) / 86400;

                if (daysSinceCommit == CONFIG.N + CONFIG.M - CONFIG.K) {
                    // Send email for pending archive tag deletion
                } else if (daysSinceCommit >= CONFIG.N + CONFIG.M) {
                    // Send email for archive tag deletion
                    git.deleteTag(t, LOGGER);
                }
            }
            LOGGER.log(Level.INFO, "Finished checking tags");
        }

        LOGGER.log(Level.INFO, "Successfully finished cleaning");
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
