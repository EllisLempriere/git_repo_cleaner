import java.util.List;

public interface IGitRemoteHandler {

    boolean cloneRepo(ILogWrapper log);

    boolean updateRepo(ILogWrapper log);

    boolean pushBranchDeletions(List<Branch> branches, ILogWrapper log);

    boolean pushNewTags(ILogWrapper log);

    boolean pushTagDeletions(List<Tag> tags, ILogWrapper log);
}
