package Application;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

public class UserCredentials {

    public final String USERNAME;
    public final String PASSWORD;

    public UserCredentials(String userInfoFile) throws UserInfoSetupException {
        try {
            File infoFile = new File(userInfoFile);
            FileReader reader = new FileReader(infoFile);

            Properties props = new Properties();
            props.load(reader);

            USERNAME = props.getProperty("username");
            PASSWORD = props.getProperty("password");

            reader.close();

        } catch (IOException e) {
            throw new UserInfoSetupException(
                    String.format("Failed to read user information from file: '%s'", userInfoFile), e);
        }
    }
}
