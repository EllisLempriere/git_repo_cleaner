import java.io.*;
import java.util.Arrays;
import java.util.Properties;

public class PropertiesTest {

    public static void main(String[] args) {
        writeProps();

        try {
            File configFile = new File("config.properties");
            FileReader reader = new FileReader(configFile);
            Properties props = new Properties();
            props.load(reader);

            String[] excludedBranches = props.getProperty("excluded branches").split(",");

            System.out.println(Arrays.toString(excludedBranches));

            reader.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void writeProps() {
        try {
            File configFile = new File("config.properties");
            FileWriter writer = new FileWriter(configFile);
            Properties props = new Properties();
            props.setProperty("excluded branches", "main");
            props.setProperty("execution time", "1685682000");
            props.store(writer, "settings");

            writer.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
