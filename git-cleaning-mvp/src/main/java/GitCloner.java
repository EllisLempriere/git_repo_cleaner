import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

import java.io.File;
import java.util.logging.Level;

public class GitCloner implements IGitCloner {

    private final String DIRECTORY;
    private final String REMOTE_URI;
    private final CredentialsProvider CREDENTIALS;
    private final int RETRIES;

    public GitCloner(Config config) {
        this.DIRECTORY = config.REPO_DIR;
        this.REMOTE_URI = config.REMOTE_URI;
        this.CREDENTIALS = new UsernamePasswordCredentialsProvider(config.USER_INFO.USERNAME, config.USER_INFO.PASSWORD);
        this.RETRIES = config.RETRIES;
    }


    @Override
    public boolean cloneRepo(ILogWrapper log) {
        String targetDirectory = DIRECTORY.substring(0, DIRECTORY.length() - 5);

        log.log(Level.INFO, String.format("Cloning from %s into %s", REMOTE_URI, targetDirectory));

        int count = 0;
        while (true) {
            try (Git git = Git.cloneRepository()
                    .setURI(REMOTE_URI)
                    .setRemote("origin")
                    .setDirectory(new File(targetDirectory))
                    .setCloneAllBranches(true)
                    .setCredentialsProvider(CREDENTIALS)
                    .call()) {

                return true;

            } catch (GitAPIException e) {
                if (++count == RETRIES) {
                    log.log(Level.SEVERE,
                            String.format("Failed to clone repo because of exception: %s. Quitting execution",
                            e.getMessage()));

                    return false;
                } else
                    log.log(Level.WARNING,
                            String.format("Repo clone failed attempt %d from exception \"%s\" - Trying again",
                            count, e.getMessage()));
            }
        }
    }
}
