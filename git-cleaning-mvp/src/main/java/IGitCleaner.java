import java.util.List;

public interface IGitCleaner {

    List<Branch> getBranches();

    List<Tag> getTags();
}
