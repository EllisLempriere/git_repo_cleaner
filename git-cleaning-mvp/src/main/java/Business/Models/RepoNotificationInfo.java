package Business.Models;

import java.util.List;

public record RepoNotificationInfo(String repoId, TakeActionCountsDays takeActionCountsDays, List<String> recipients) {
}
