package Application.Models;

import java.util.List;

public record Configs(Integer retries, ConfigSecrets config_secrets, List<RepoConfig> repos) {
}
