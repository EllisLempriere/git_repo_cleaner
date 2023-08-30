package Application;

import Application.Models.Configs;
import Application.Models.ConfigsSetupException;
import Application.Models.RepoConfig;
import Business.GitRepoCleanerLogic;
import Business.IGitRepoCleanerLogic;
import Business.INotificationLogic;
import Business.Models.RepoCleaningInfo;
import Business.Models.RepoNotificationInfo;
import Business.Models.TakeActionCountsDays;
import Business.NotificationLogic;
import Provider.EmailProvider;
import Provider.GitProvider;
import Provider.IEmailProvider;
import Provider.IGitProvider;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

public class Main {

    public static void main(String[] args) {
        ICustomLogger logger = new CustomLogger();
        logger.log(Level.INFO, "Beginning program");

        int executionTime = Integer.parseInt(args[0]);
        String configFile = args[1];

        if (executionTime == 0)
            executionTime = (int) Instant.now().getEpochSecond();

        try {
            ConfigGetter configGetter = new ConfigGetter(configFile);
            Configs configs = configGetter.getConfigs();

            logger.log(Level.INFO, String.format("Reading in global configs: retries=%d, secrets_file=%s",
                    configs.retries(), configs.config_secrets().FILE));

            List<RepoCleaningInfo> repoCleaningInfoList = new ArrayList<>(configs.repos().size());
            List<RepoNotificationInfo> repoNotificationInfoList = new ArrayList<>(configs.repos().size());
            int i = 0;
            for (RepoConfig repo : configs.repos()) {
                TakeActionCountsDays actionCountsDays = new TakeActionCountsDays(
                        repo.stale_branch_inactivity_days(), repo.stale_tag_days(), repo.notification_before_action_days());

                repoCleaningInfoList.add(new RepoCleaningInfo(repo.remote_uri(), repo.directory(), repo.remote_uri(),
                        repo.excluded_branches(), actionCountsDays));

                repoNotificationInfoList.add(new RepoNotificationInfo(repo.remote_uri(), actionCountsDays,
                        repo.recipients()));

                logger.log(Level.INFO,
                        String.format("Reading in repo %d configs: directory=%s, remote_uri=%s, excluded_branches=%s, " +
                        "stale_branch_inactivity_days=%d, stale_tag_days=%d, notification_before_action_days=%d, " +
                        "recipients=%s", ++i, repo.directory(), repo.remote_uri(), repo.excluded_branches(),
                        repo.stale_branch_inactivity_days(), repo.stale_tag_days(), repo.notification_before_action_days(),
                        repo.recipients()));
            }

            IEmailProvider emailProvider = new EmailProvider();
            INotificationLogic notificationLogic = new NotificationLogic(emailProvider, repoNotificationInfoList, logger);

            IGitProvider gitProvider = new GitProvider(configs.config_secrets(), configs.retries());
            IGitRepoCleanerLogic gitRepoCleanerLogic = new GitRepoCleanerLogic(repoCleaningInfoList,
                    gitProvider, notificationLogic, logger, executionTime);

            logger.log(Level.INFO, "Starting cleaning on " + configs.repos().size() + " repo(s)");
            gitRepoCleanerLogic.cleanRepos();

            logger.log(Level.INFO, "Ending program");

        } catch (ConfigsSetupException e) {
            logger.logError("Issue in reading config, halting execution. Error: " + e.getMessage());
        } catch (RuntimeException e) {
            logger.logError("Unexpected error, halting execution. Error: " + e.getMessage());
        }
    }
}
