import java.util.List;

// Needs a rename
public interface IGitCleaner {

    List<Branch> getBranches();

    List<Tag> getTags();

    void archiveBranch(Branch branch);

    void deleteTag(Tag tag);
}
