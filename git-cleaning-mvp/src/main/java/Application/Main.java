package Application;

import Application.Models.Configs;
import Application.Models.ConfigsSetupException;
import Business.*;
import Business.Models.TakeActionCountsDays;
import Provider.EmailProvider;
import Provider.GitProvider;
import Provider.IEmailProvider;
import Provider.IGitProvider;

import java.util.logging.Level;

public class Main {

    public static void main(String[] args) {
        ICustomLogger logger = new CustomLogger();
        logger.log(Level.INFO, "Beginning program");

        try {
            logger.log(Level.INFO, "Starting set up");
            ConfigGetter configGetter = new ConfigGetter("config.json");
            Configs configs = configGetter.getConfigs();


            IEmailProvider emailProvider = new EmailProvider();
            //INotificationLogic notificationLogic = new NotificationLogic(configs.daysToActions(), emailProvider);
            //IGitProvider gitProvider = new GitProvider(configs.configSecrets(), configs.retries());
            //IGitRepoCleanerLogic gitRepoCleanerLogic = new GitRepoCleanerLogic(configs.daysToActions(),
            //        gitProvider, notificationLogic, logger);
            logger.log(Level.INFO, "Finished bootstrapping");

            //logger.log(Level.INFO, "Starting cleaning " + configs.repos().size() + " repos");
            //gitRepoCleanerLogic.cleanRepos(configs.repos());

        } catch (ConfigsSetupException e) {
            logger.log(Level.SEVERE, "Issue in reading config, halting execution");
            logger.log(Level.SEVERE, "Error: " + e.getMessage());
        }
    }
}
