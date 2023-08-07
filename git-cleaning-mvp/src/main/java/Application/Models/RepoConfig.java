package Application.Models;

import java.util.List;

public record RepoConfig(String directory, String remote_uri, List<String> excluded_branches,
                         int stale_branch_inactivity_days, int stale_tag_days, int notification_before_action_days,
                         List<String> recipients) {
}
