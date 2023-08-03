package Application;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

public class Config {

    public final int DAYS_TO_STALE_BRANCH;
    public final int DAYS_TO_STALE_TAG;
    public final int PRECEDING_DAYS_TO_WARN;
    public final List<String> EXCLUDED_BRANCHES;
    public final String REPO_DIR;
    public final String REMOTE_URI;
    public final UserCredentials USER_INFO;
    public final int RETRIES;

    public Config(String propertiesFile) throws ConfigSetupException {
        try {
            File configFile = new File(propertiesFile);
            FileReader reader = new FileReader(configFile);

            Properties props = new Properties();
            props.load(reader);

            DAYS_TO_STALE_BRANCH = Integer.parseInt(props.getProperty("n"));
            DAYS_TO_STALE_TAG = Integer.parseInt(props.getProperty("m"));
            PRECEDING_DAYS_TO_WARN = Integer.parseInt(props.getProperty("k"));
            EXCLUDED_BRANCHES = Arrays.asList(props.getProperty("excluded branches").split(","));
            REPO_DIR = props.getProperty("repo directory");
            REMOTE_URI = props.getProperty("remote uri");
            USER_INFO = new UserCredentials(props.getProperty("user info"));
            RETRIES = Integer.parseInt(props.getProperty("retries"));

            reader.close();

        } catch (IOException e) {
            throw new ConfigSetupException(
                    String.format("Failed to set up application config from file: '%s'", propertiesFile), e);
        } catch (UserInfoSetupException e) {
            throw new ConfigSetupException(
                    String.format("Failed to read config because: %s", e.getMessage()), e);
        }
    }
}
