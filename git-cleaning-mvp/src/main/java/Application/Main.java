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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;

public class Main {

    public static void main(String[] args) {
        ICustomLogger logger = new CustomLogger();
        logger.log(Level.INFO, "Beginning program");

        int executionTime = Integer.parseInt(args[0]);

        try {
            logger.log(Level.INFO, "Starting set up");
            ConfigGetter configGetter = new ConfigGetter("config.json");
            Configs configs = configGetter.getConfigs();

            List<RepoCleaningInfo> repoCleaningInfoList = new ArrayList<>(configs.repos().size());
            List<RepoNotificationInfo> repoNotificationInfoList = new ArrayList<>(configs.repos().size());
            for (RepoConfig repo : configs.repos()) {
                TakeActionCountsDays actionCountsDays = new TakeActionCountsDays(
                        repo.stale_branch_inactivity_days(), repo.stale_tag_days(), repo.notification_before_action_days());

                repoCleaningInfoList.add(new RepoCleaningInfo(repo.remote_uri(), repo.directory(), repo.remote_uri(),
                        repo.excluded_branches(), actionCountsDays));

                repoNotificationInfoList.add(new RepoNotificationInfo(repo.remote_uri(), actionCountsDays,
                        repo.recipients()));
            }

            IEmailProvider emailProvider = new EmailProvider();
            INotificationLogic notificationLogic = new NotificationLogic(emailProvider, repoNotificationInfoList);

            IGitProvider gitProvider = new GitProvider(configs.config_secrets(), configs.retries());
            IGitRepoCleanerLogic gitRepoCleanerLogic = new GitRepoCleanerLogic(repoCleaningInfoList,
                    gitProvider, notificationLogic, logger, executionTime);
            logger.log(Level.INFO, "Finished bootstrapping");

            logger.log(Level.INFO, "Starting cleaning on " + configs.repos().size() + " repo(s)");
            gitRepoCleanerLogic.cleanRepos();

        } catch (ConfigsSetupException e) {
            logger.log(Level.SEVERE, "Issue in reading config, halting execution");
            logger.log(Level.SEVERE, "Error: " + Arrays.toString(e.getStackTrace()));
        }
    }
}
