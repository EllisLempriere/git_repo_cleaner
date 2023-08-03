package Application;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

public class ConfigSecrets {

    public final String USERNAME;
    public final String PASSWORD;

    public ConfigSecrets(String userInfoFile) throws ConfigsSetupException {
        try {
            File infoFile = new File(userInfoFile);
            FileReader reader = new FileReader(infoFile);

            Properties props = new Properties();
            props.load(reader);

            USERNAME = props.getProperty("username");
            PASSWORD = props.getProperty("password");

            reader.close();

        } catch (IOException e) {
            throw new ConfigsSetupException(
                    String.format("Failed to read config secrets because: %s", e.getMessage()), e);
        }
    }
}
