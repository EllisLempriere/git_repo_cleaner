import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

public class Config {

    public final int N;
    public final int M;
    public final int K;
    public final List<String> EXCLUDED_BRANCHES;
    public final String REPO_DIR;


    // Should programmatically writing to config be available?
    public Config(String propertiesFile) {
        try {
            // Should the name of the config file be hardcoded or passed in?
            File configFile = new File(propertiesFile);
            FileReader reader = new FileReader(configFile);

            Properties props = new Properties();
            props.load(reader);

            N = Integer.parseInt(props.getProperty("n"));
            M = Integer.parseInt(props.getProperty("m"));
            K = Integer.parseInt(props.getProperty("k"));
            EXCLUDED_BRANCHES = Arrays.asList(props.getProperty("excluded branches").split(","));
            REPO_DIR = props.getProperty("repo directory");

            reader.close();
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
