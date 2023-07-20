import java.util.List;

public class Client {

    private static Config config;

    // Hardcoded for testing. Will pull from current time in final implementation
    private static final int executionTime = 1685682000;

    public static void main(String[] args) {
        config = new Config("default_config.properties");

        GitCleaner cleaner = new GitCleaner(config.REPO_DIR);

        List<Branch> branches = cleaner.getBranches();
        List<Tag> tags = cleaner.getTags();

        for (Branch b : branches) {
            if (!config.EXCLUDED_BRANCHES.contains(b.name())) {
                int commitTime = b.commits().get(0).commitTime();

                if (executionTime - commitTime == config.N - config.K) {
                    // Will send email
                } else if (executionTime - commitTime >= config.N) {
                    // Will send email
                    cleaner.archiveBranch(b);
                }
            }
        }

        for (Tag t : tags) {
            if (t.name().matches("zArchiveBranch_\\d{8}_\\w*")) {
                int commitTime = t.commit().commitTime();

                if (executionTime - commitTime == config.N + config.M - config.K) {
                    // Will send email
                } else if (executionTime - commitTime >= config.N + config.M) {
                    // Will send email
                    cleaner.deleteTag(t);
                }
            }
        }
    }
}
