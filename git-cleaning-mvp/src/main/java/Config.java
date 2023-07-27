import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

// Does this class need more than just loading in config?
// How does a user do initial setup and edits to config?
public class Config {

    public final int N;
    public final int M;
    public final int K;
    public final List<String> EXCLUDED_BRANCHES;
    public final String REPO_DIR;
    public final String REMOTE_URI;
    public final UserInfo USER_INFO;
    public final int RETRIES;


    // TODO - Take care of case when config setup fails - logging needed
    // Should programmatically writing to config be available?
    public Config(String propertiesFile) {
        try {
            File configFile = new File(propertiesFile);
            FileReader reader = new FileReader(configFile);

            Properties props = new Properties();
            props.load(reader);

            N = Integer.parseInt(props.getProperty("n"));
            M = Integer.parseInt(props.getProperty("m"));
            K = Integer.parseInt(props.getProperty("k"));
            EXCLUDED_BRANCHES = Arrays.asList(props.getProperty("excluded branches").split(","));
            REPO_DIR = props.getProperty("repo directory");
            REMOTE_URI = props.getProperty("remote uri");
            USER_INFO = new UserInfo(props.getProperty("user info"));
            RETRIES = Integer.parseInt(props.getProperty("retries"));

            reader.close();

        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
