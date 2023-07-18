import java.io.FileInputStream;
import java.io.IOException;
import java.util.logging.*;

public class LoggerTest {

    private static Logger logger = Logger.getLogger(LoggerTest.class.getName());

    public static void main(String[] args) {
        try {
            LogManager.getLogManager().readConfiguration(new FileInputStream("logging.properties"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        logger.setLevel(Level.FINE);
        logger.addHandler(new ConsoleHandler());

        try {
            Handler fileHandler = new FileHandler("C:\\Users\\ellis\\Documents\\repos\\git_repo_cleaner\\JavaShellCommands\\LogFile.log", 5000, 1);
            fileHandler.setFormatter(new SimpleFormatter());
            logger.addHandler(fileHandler);

            for (int i = 0; i < 50; i++)
                logger.log(Level.INFO, "Msg" + i);

            logger.log(Level.CONFIG, "Done");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
