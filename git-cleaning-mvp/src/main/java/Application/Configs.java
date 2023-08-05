package Application;

import Business.Models.DaysToActions;
import Business.Models.RepoInfo;

import java.util.List;

public record Configs(DaysToActions daysToActions, int retries, ConfigSecrets configSecrets,
                      List<String> recipients, List<RepoInfo> repos) {
}
