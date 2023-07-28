import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ListBranchCommand;
import org.eclipse.jgit.api.LogCommand;
import org.eclipse.jgit.api.MergeCommand;
import org.eclipse.jgit.api.errors.*;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;


public class GitWrapper implements IGitWrapper {

    private final Config CONFIG;
    private final CredentialsProvider CREDENTIALS;
    private Repository repo;
    private Git git;

    public GitWrapper(Config config) {
        this.CONFIG = config;
        this.CREDENTIALS = new UsernamePasswordCredentialsProvider(config.USER_INFO.USERNAME, config.USER_INFO.PASSWORD);
    }


    @Override
    public boolean startGit(ILogWrapper log) {
        log.log(Level.INFO, "Starting up git");

        int count = 0;
        while (true) {
            FileRepositoryBuilder repoBuilder = new FileRepositoryBuilder();
            try (Repository repo = repoBuilder
                    .setGitDir(new File(CONFIG.REPO_DIR))
                    .readEnvironment()
                    .findGitDir()
                    .build();
                Git git = new Git(repo)) {

                this.repo = repo;
                this.git = git;

                return true;

            } catch (IOException e) {
                if (++count == CONFIG.RETRIES) {
                    log.log(Level.SEVERE,
                            String.format("Failed to start git because of exception: %s. Quitting execution",
                            e.getMessage()));

                    return false;
                } else
                    log.log(Level.WARNING,
                            String.format("Git startup failed attempt %d from exception \"%s\" - Trying again",
                            count, e.getMessage()));
            }
        }
    }

    // TODO - Account for empty repo
    @Override
    public boolean updateRepo(ILogWrapper log) {
        log.log(Level.INFO,
                String.format("Updating local repo at %s from remote repo %s", CONFIG.REPO_DIR, CONFIG.REMOTE_URI));

        int count = 0;
        while (true) {
            try {
                // TODO - Bug on retry
                git.pull()
                        .setFastForward(MergeCommand.FastForwardMode.FF_ONLY)
                        .setCredentialsProvider(CREDENTIALS)
                        .call();

                List<Ref> remoteBranches = git.branchList()
                        .setListMode(ListBranchCommand.ListMode.REMOTE)
                        .call();
                List<Ref> localBranches = git.branchList()
                        .call();

                List<Ref> remoteOnlyBranches = getRemoteOnlyBranches(remoteBranches, localBranches);
                for (Ref b : remoteOnlyBranches) {
                    git.checkout().setName(b.getName()).call();
                    git.branchCreate().setName(getRefName(b)).call();
                }

                String trunkBranch = getRefName(remoteBranches.get(remoteBranches.size() - 1));
                git.checkout().setName(trunkBranch).call();

                return true;

            } catch (GitAPIException e) {
                if (++count == CONFIG.RETRIES) {
                    log.log(Level.SEVERE,
                            String.format("Failed to update local repo because of exception: %s. Quitting execution",
                            e.getMessage()));

                    return false;
                } else
                    log.log(Level.WARNING,
                            String.format("Local repo update failed attempt %d from exception \"%s\" - Trying again",
                            count, e.getMessage()));
            }
        }
    }

    @Override
    public List<Branch> getBranches(ILogWrapper log) {
        log.log(Level.INFO, "Fetching branch list");
        List<Branch> branches = new ArrayList<>();

        int count = 0;
        while (true) {
            try {
                List<Ref> rawBranches = git.branchList().call();
                for (Ref b : rawBranches) {
                    String branchName = getRefName(b);
                    List<Commit> commits = getBranchCommitList(branchName);

                    branches.add(new Branch(branchName, commits));
                }

                return branches;

            } catch (GitAPIException | IOException e) {
                if (++count == CONFIG.RETRIES) {
                    log.log(Level.SEVERE,
                            String.format("Failed to fetch branch list because of exception: %s. Quitting execution",
                            e.getMessage()));

                    return null;
                } else
                    log.log(Level.WARNING,
                            String.format("Branch list fetch failed attempt %d from exception \"%s\" - Trying again",
                            count, e.getMessage()));
            }
        }
    }

    @Override
    public List<Tag> getTags(ILogWrapper log) {
        log.log(Level.INFO, "Fetching tag list");
        List<Tag> tags = new ArrayList<>();

        int count = 0;
        while (true) {
            try {
                List<Ref> rawTags = git.tagList().call();

                for (Ref t : rawTags) {
                    String tagName = getRefName(t);
                    Commit commit = getTagCommit(t);

                    tags.add(new Tag(tagName, commit));
                }

                return tags;

            } catch (GitAPIException | IOException e) {
                if (++count == CONFIG.RETRIES) {
                    log.log(Level.SEVERE,
                            String.format("Failed to fetch tag list because of exception: %s. Quitting execution",
                            e.getMessage()));

                    return null;
                } else
                    log.log(Level.WARNING,
                            String.format("Tag list fetch failed attempt %d from exception \"%s\" - Trying again",
                            count, e.getMessage()));
            }
        }
    }

    @Override
    public boolean setTag(Tag tag, ILogWrapper log) {
        log.log(Level.INFO, "Adding tag " + tag.name());

        int count = 0;
        while (true) {
            try {
                RevCommit commit = git.log().add(repo.resolve(tag.commit().commitId())).call().iterator().next();
                git.tag().setName(tag.name()).setObjectId(commit).call();

                return true;

            } catch (GitAPIException | IOException e) {
                if (++count == CONFIG.RETRIES) {
                    log.log(Level.SEVERE,
                            String.format("Failed to set tag because of exception: %s. Skipping archival for branch",
                            e.getMessage()));

                    return false;
                } else
                    log.log(Level.WARNING,
                            String.format("Tag setting failed attempt %d from exception \"%s\" - Trying again",
                            count, e.getMessage()));
            }
        }
    }

    @Override
    public boolean deleteBranch(Branch branch, ILogWrapper log) {
        log.log(Level.INFO, "Deleting branch " + branch.name());

        int count = 0;
        while (true) {
            try {
                git.branchDelete().setBranchNames(branch.name()).setForce(true).call();

                return true;

            } catch (GitAPIException e) {
                if (++count == CONFIG.RETRIES) {
                    log.log(Level.SEVERE,
                            String.format("Failed to delete branch because of exception: %s. Skipping archival for branch",
                            e.getMessage()));

                    return false;
                } else
                    log.log(Level.WARNING,
                            String.format("Branch deletion failed attempt %d from exception \"%s\" - Trying again",
                            count, e.getMessage()));
            }
        }
    }

    @Override
    public boolean deleteTag(Tag tag, ILogWrapper log) {
        log.log(Level.INFO, "Deleting tag " + tag.name());

        int count = 0;
        while (true) {
            try {
                git.tagDelete().setTags(tag.name()).call();

                return true;

            } catch (GitAPIException e) {
                if (++count == CONFIG.RETRIES) {
                    log.log(Level.SEVERE,
                            String.format("Failed to delete tag because of exception: %s",
                            e.getMessage()));

                    return false;
                } else
                    log.log(Level.WARNING,
                            String.format("Tag deletion failed attempt %d from exception \"%s\" - Trying again",
                            count, e.getMessage()));
            }
        }
    }

    @Override
    public void pushDeletedBranch(Branch branch, ILogWrapper log) {
        int count = 0;
        while (true) {
            try {
                RefSpec refSpec = new RefSpec()
                        .setSource(null)
                        .setDestination("refs/heads/" + branch.name());

                git.push()
                        .setRefSpecs(refSpec)
                        .setRemote("origin")
                        .setCredentialsProvider(CREDENTIALS)
                        .call();

                log.log(Level.INFO,
                        String.format("Successfully updated remote with deletion of branch %s", branch.name()));
                return;

            } catch (GitAPIException e) {
                if (++count == CONFIG.RETRIES) {
                    log.log(Level.SEVERE,
                            String.format("Failed to push deletion of branch %s because of exception: %s",
                            branch.name(), e.getMessage()));

                    return;
                } else
                    log.log(Level.WARNING,
                            String.format("Deletion of branch %s push failed attempt %d from exception \"%s\" - Trying again",
                            branch.name(), count, e.getMessage()));
            }
        }
    }

    @Override
    public void pushNewTags(ILogWrapper log) {
        int count = 0;
        while (true) {
            try {
                List<Ref> tags = git.tagList().call();

                if (!tags.isEmpty())
                    git.push()
                            .setPushTags()
                            .setRemote("origin")
                            .setCredentialsProvider(CREDENTIALS)
                            .call();

                log.log(Level.INFO, "Successfully updated remote with newly created tags");
                return;

            } catch (GitAPIException e) {
                if (++count == CONFIG.RETRIES) {
                    log.log(Level.SEVERE,
                            String.format("Failed to push new tags because of exception: %s",
                            e.getMessage()));

                    return;
                } else
                    log.log(Level.WARNING,
                            String.format("New tags push failed attempt %d from exception \"%s\" - Trying again",
                            count, e.getMessage()));
            }
        }
    }

    @Override
    public void pushDeletedTag(Tag tag, ILogWrapper log) {
        int count = 0;
        while (true) {
            try {
                RefSpec refSpec = new RefSpec()
                        .setSource(null)
                        .setDestination("refs/tags/" + tag.name());

                git.push()
                        .setRefSpecs(refSpec)
                        .setRemote("origin")
                        .setCredentialsProvider(CREDENTIALS)
                        .call();

                return;

            } catch (GitAPIException e) {
                if (++count == CONFIG.RETRIES) {
                    log.log(Level.SEVERE,
                            String.format("Failed to push deletion of tag %s because of exception: %s",
                            tag.name(), e.getMessage()));

                    return;
                } else
                    log.log(Level.WARNING,
                            String.format("Deletion of tag %s push failed attempt %d from exception \"%s\" - Trying again",
                            tag.name(), count, e.getMessage()));
            }
        }
    }


    private String getRefName(Ref ref) {
        String[] refPath = ref.getName().split("/");
        return refPath[refPath.length - 1];
    }

    private List<Ref> getRemoteOnlyBranches(List<Ref> remoteBranches, List<Ref> localBranches) {
        List<Ref> onlyRemoteBranches = new ArrayList<>();

        for (Ref rb : remoteBranches) {
            boolean branchOnBoth = false;
            String remoteBranchName = getRefName(rb);
            for (Ref lb : localBranches) {
                String localBranchName = getRefName(lb);
                if (remoteBranchName.equals(localBranchName)) {
                    branchOnBoth = true;
                    break;
                }
            }
            if (!branchOnBoth)
                onlyRemoteBranches.add(rb);
        }

        return onlyRemoteBranches;
    }

    private List<Commit> getBranchCommitList(String branchName) throws IOException, GitAPIException {
        List<Commit> commits = new ArrayList<>();

        Iterable<RevCommit> rawCommits = git.log().add(repo.resolve(branchName)).call();
        for (RevCommit c : rawCommits)
            commits.add(parseRevCommit(c));

        return commits;
    }

    private Commit getTagCommit(Ref tag) throws IOException, GitAPIException {
        LogCommand logCommand = git.log();

        Ref peeledRef = repo.getRefDatabase().peel(tag);
        if (peeledRef.getPeeledObjectId() != null)
            logCommand.add(peeledRef.getPeeledObjectId());
        else
            logCommand.add(tag.getObjectId());

        Iterable<RevCommit> tagLog = logCommand.call();

        return parseRevCommit(tagLog.iterator().next());
    }

    private Commit parseRevCommit(RevCommit rawCommit) {
        String commitId = rawCommit.getId().getName();

        int commitTime = (int) rawCommit.getAuthorIdent().getWhenAsInstant().getEpochSecond();

        PersonIdent rawAuthor = rawCommit.getAuthorIdent();
        CommitAuthor author = new CommitAuthor(rawAuthor.getName(), rawAuthor.getEmailAddress());

        return new Commit(commitId, commitTime, author);
    }
}
