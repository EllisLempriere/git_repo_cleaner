package Provider;

import Application.UserInfo;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

import java.io.File;

public class GitCloner implements IGitCloner {

    private final String DIRECTORY;
    private final String REMOTE_URI;
    private final CredentialsProvider CREDENTIALS;
    private final int RETRIES;

    public GitCloner(String repoDirectory, String remoteUri, UserInfo user, int retries) {
        this.DIRECTORY = repoDirectory;
        this.REMOTE_URI = remoteUri;
        this.CREDENTIALS = new UsernamePasswordCredentialsProvider(user.USERNAME, user.PASSWORD);
        this.RETRIES = retries;
    }


    @Override
    public void cloneRepo() throws GitCloningException {
        String targetDirectory = DIRECTORY.substring(0, DIRECTORY.length() - 5);

        int count = 0;
        while (true) {
            try (Git git = Git.cloneRepository()
                    .setURI(REMOTE_URI)
                    .setRemote("origin")
                    .setDirectory(new File(targetDirectory))
                    .setCloneAllBranches(true)
                    .setCredentialsProvider(CREDENTIALS)
                    .call()) {

                return;

            } catch (GitAPIException e) {
                if (++count == RETRIES) {
                    throw new GitCloningException(
                            String.format("Failed to clone repo after %d attempts due to %s", RETRIES, e.getMessage()),
                            e);
                }
            }
        }
    }
}
