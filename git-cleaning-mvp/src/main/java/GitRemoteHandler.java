import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ListBranchCommand;
import org.eclipse.jgit.api.MergeCommand;
import org.eclipse.jgit.api.errors.*;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;

public class GitRemoteHandler implements IGitRemoteHandler {

    private final String FILE_PATH;
    private final String REMOTE_URI;
    private final CredentialsProvider credentials;

    public GitRemoteHandler(String filePath, String remoteUri, UserInfo user) {
        this.FILE_PATH = filePath;
        this.REMOTE_URI = remoteUri;

        credentials = new UsernamePasswordCredentialsProvider(user.USERNAME, user.PASSWORD);
    }

    @Override
    public boolean cloneRepo(ILogWrapper log) {
        String directory = FILE_PATH.substring(0, FILE_PATH.length() - 5);

        try (Git git = Git.cloneRepository()
                .setURI(REMOTE_URI)
                .setRemote("origin")
                .setDirectory(new File(directory))
                .setCloneAllBranches(true)
                .setCredentialsProvider(credentials)
                .call()) {

            return true;

        } catch (GitAPIException e) {
            return false;
        }
    }

    @Override
    public boolean updateRepo(ILogWrapper log) {
        try (Git git = Git.open(new File(FILE_PATH))) {
            git.pull()
                    .setFastForward(MergeCommand.FastForwardMode.FF_ONLY)
                    .setCredentialsProvider(credentials)
                    .call();

            List<Ref> remoteBranches = git.branchList()
                    .setListMode(ListBranchCommand.ListMode.REMOTE)
                    .call();
            List<Ref> localBranches = git.branchList().call();
            boolean branchOnLocal = false;
            for (Ref rb : remoteBranches) {
                for (Ref lb : localBranches) {
                    if (rb.getObjectId().compareTo(lb.getObjectId()) == 0) {
                        branchOnLocal = true;
                        break;
                    }
                }
                if (!branchOnLocal) {
                    git.checkout().setName(rb.getName()).call();

                    String[] refPath = rb.getName().split("/");
                    String branchName = refPath[refPath.length - 1];
                    git.branchCreate().setName(branchName).call();
                }
                branchOnLocal = false;
            }
            String[] refPath = remoteBranches.get(remoteBranches.size() - 1).getName().split("/");
            String trunkBranchName = refPath[refPath.length - 1];

            git.checkout().setName(trunkBranchName).call();

            return true;

        } catch (IOException | GitAPIException e) {
            return false;
        }
    }

    @Override
    public boolean hasRemote(ILogWrapper log) {
        try (Git git = Git.open(new File(FILE_PATH))) {
            List<RemoteConfig> remotes = git.remoteList().call();

            return !remotes.isEmpty();

        } catch (IOException | GitAPIException e) {
            return false;
        }
    }

    @Override
    public void addRemote(ILogWrapper log) {
        try (Git git = Git.open(new File(FILE_PATH))) {
            git.remoteAdd()
                    .setUri(new URIish(REMOTE_URI))
                    .setName("origin")
                    .call();

        } catch (IOException | GitAPIException | URISyntaxException ignored) {
        }
    }
}
