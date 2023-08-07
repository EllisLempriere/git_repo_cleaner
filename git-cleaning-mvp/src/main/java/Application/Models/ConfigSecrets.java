package Application.Models;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

public class ConfigSecrets {

    public final String USERNAME;
    public final String PASSWORD;

    public ConfigSecrets(String fileName) throws ConfigsSetupException {
        if (fileName == null)
            throw new ConfigsSetupException("Config secrets file name cannot be null");

        try {
            File infoFile = new File(fileName);
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

        if (USERNAME == null)
            throw new ConfigsSetupException("Secrets file missing username information");
        else if (PASSWORD == null)
            throw new ConfigsSetupException("Secrets file missing password information");
    }

    public ConfigSecrets(String username, String password) {
        this.USERNAME = username;
        this.PASSWORD = password;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null)
            return false;

        if (obj.getClass() != this.getClass())
            return false;

        final ConfigSecrets other = (ConfigSecrets) obj;

        if (!this.USERNAME.equals(other.USERNAME))
            return false;

        if (!this.PASSWORD.equals(other.PASSWORD))
            return false;

        return true;
    }
}
