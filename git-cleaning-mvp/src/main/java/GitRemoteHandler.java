import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ListBranchCommand;
import org.eclipse.jgit.api.MergeCommand;
import org.eclipse.jgit.api.errors.*;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.transport.*;

import java.io.File;
import java.io.IOException;
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
    public boolean pushBranchDeletions(List<Branch> branches, ILogWrapper log) {
        try (Git git = Git.open(new File(FILE_PATH))) {
            for (Branch b : branches) {
                RefSpec refSpec = new RefSpec()
                        .setSource(null)
                        .setDestination("refs/heads/" + b.name());

                git.push()
                        .setRefSpecs(refSpec)
                        .setRemote("origin")
                        .setCredentialsProvider(credentials)
                        .call();
            }

            return true;

        } catch (IOException | GitAPIException e) {
            return false;
        }
    }

    @Override
    public boolean pushNewTags(ILogWrapper log) {
        try (Git git = Git.open(new File(FILE_PATH))) {
            List<Ref> tags = git.tagList().call();

            if (!tags.isEmpty())
                git.push()
                    .setPushTags()
                    .setRemote("origin")
                    .setCredentialsProvider(credentials)
                    .call();

            return true;

        } catch (IOException | GitAPIException e) {
            return false;
        }
    }

    @Override
    public boolean pushTagDeletions(List<Tag> tags, ILogWrapper log) {
        try (Git git = Git.open(new File(FILE_PATH))) {
            for (Tag t : tags) {
                RefSpec refSpec = new RefSpec()
                        .setSource(null)
                        .setDestination("refs/tags/" + t.name());

                git.push()
                        .setRefSpecs(refSpec)
                        .setRemote("origin")
                        .setCredentialsProvider(credentials)
                        .call();
            }

            return true;

        } catch (IOException | GitAPIException e) {
            return false;
        }
    }
}
