package Business.Models;

import java.util.List;

public record RepoInfo(String repoDir, String remoteUri, List<String> excludedBranches) {
}
