import java.util.List;

public class Client {

    // All to be refactored into configuration later
    private static final String repoPath = "C:\\Users\\ellis\\Documents\\repos\\test-case-5\\.git";
    private static final int n = 60;
    private static final int m = 30;
    private static final int k = 7;
    private static final int executionTime = 1685682000;

    public static void main(String[] args) {
        GitCleaner cleaner = new GitCleaner(repoPath);

        List<Branch> branches = cleaner.getBranches();
        List<Tag> tags = cleaner.getTags();

        for (Branch b : branches) {
            int commitTime = b.commits().get(0).commitTime();

            if (executionTime - commitTime == n - k) {

            } else if (executionTime - commitTime >= n) {

            }
        }
    }
}
