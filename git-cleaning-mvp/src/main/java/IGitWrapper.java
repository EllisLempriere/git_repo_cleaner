import java.util.List;

public interface IGitWrapper {

    boolean startGit(ILogWrapper log);

    boolean updateRepo(ILogWrapper log);

    List<Branch> getBranches(ILogWrapper log);

    List<Tag> getTags(ILogWrapper log);

    boolean setTag(Tag tag, ILogWrapper log);

    boolean deleteBranch(Branch branch, ILogWrapper log);

    boolean deleteTag(Tag tag, ILogWrapper log);

    void pushDeletedBranch(Branch branch, ILogWrapper log);

    void pushNewTags(ILogWrapper log);

    void pushDeletedTag(Tag tag, ILogWrapper log);
}
