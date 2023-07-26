import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

public class UserInfo {

    public final String USERNAME;
    public final String PASSWORD;


    public UserInfo(String userInfoFile) {
        try {
            File infoFile = new File(userInfoFile);
            FileReader reader = new FileReader(infoFile);

            Properties props = new Properties();
            props.load(reader);

            USERNAME = props.getProperty("username");
            PASSWORD = props.getProperty("password");

            reader.close();

        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
