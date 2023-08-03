package Application;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

public class Configs {

    public final int DAYS_TO_STALE_BRANCH;
    public final int DAYS_TO_STALE_TAG;
    public final int PRECEDING_DAYS_TO_WARN;
    public final List<String> EXCLUDED_BRANCHES;
    public final String REPO_DIR;
    public final String REMOTE_URI;
    public final ConfigSecrets CONFIG_SECRETS;
    public final int RETRIES;

    public Configs(String propertiesFile) throws ConfigsSetupException {
        try {
            File configFile = new File(propertiesFile);
            FileReader reader = new FileReader(configFile);

            Properties props = new Properties();
            props.load(reader);

            DAYS_TO_STALE_BRANCH = Integer.parseInt(props.getProperty("days to stale branch"));
            DAYS_TO_STALE_TAG = Integer.parseInt(props.getProperty("days to stale tag"));
            PRECEDING_DAYS_TO_WARN = Integer.parseInt(props.getProperty("preceding days to warn"));
            EXCLUDED_BRANCHES = Arrays.asList(props.getProperty("excluded branches").split(","));
            REPO_DIR = props.getProperty("repo directory");
            REMOTE_URI = props.getProperty("remote uri");
            CONFIG_SECRETS = new ConfigSecrets(props.getProperty("config secrets"));
            RETRIES = Integer.parseInt(props.getProperty("retries"));

            reader.close();

        } catch (IOException e) {
            throw new ConfigsSetupException(
                    String.format("Failed to set up application config from file: '%s'", propertiesFile), e);
        }
    }
}
