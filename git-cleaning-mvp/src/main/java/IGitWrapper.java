import java.util.List;

public interface IGitWrapper {

    List<Branch> getBranches(ILogWrapper log);

    List<Tag> getTags(ILogWrapper log);

    boolean setTag(Tag tag, ILogWrapper log);

    void deleteBranch(Branch branch, ILogWrapper log);

    void deleteTag(Tag tag, ILogWrapper log);
}
