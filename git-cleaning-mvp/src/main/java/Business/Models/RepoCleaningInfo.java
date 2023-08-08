package Business.Models;

import java.util.List;

public record RepoCleaningInfo(String repoId, String repoDir, String remoteUri,
                               List<String> excludedBranches, TakeActionCountsDays takeActionCountsDays) {
}
