package Application;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.logging.*;

public class CustomLogger implements ICustomLogger {

    private final Logger LOGGER = Logger.getLogger(CustomLogger.class.getName());

    public CustomLogger() {
        LOGGER.setLevel(Level.FINE);

        DateFormat dateFormat = new SimpleDateFormat("ddMMyyyy");
        int logNum = 0;
        String logName = dateFormat.format(System.currentTimeMillis()) + "Log-" + logNum + ".log";
        Path logPath = Paths.get(
                "C:\\Users\\ellis\\Documents\\repos\\git_repo_cleaner\\git-cleaning-mvp\\Outputs\\Logs\\" + logName);

        while (Files.exists(logPath)){
            logNum++;
            logName = dateFormat.format(System.currentTimeMillis()) + "Log-" + logNum + ".log";
            logPath = Paths.get(
                    "C:\\Users\\ellis\\Documents\\repos\\git_repo_cleaner\\git-cleaning-mvp\\Outputs\\Logs\\" + logName);
        }

        try {
            Handler handler = new FileHandler(logName, 5000000, 1);
            handler.setFormatter(new CustomLogFormatter());

            LOGGER.setUseParentHandlers(false);
            LOGGER.addHandler(handler);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void log(Level level, String message) {
        LOGGER.log(level, message);
    }

    @Override
    public void logRepoMsg(String message, int repoNum) {
        String prefix = String.format("R%s - ", padNum(repoNum));

        log(Level.INFO, prefix + message);
    }

    @Override
    public void logBranchMsg(String message, int repoNum, int branchNum) {
        String prefix = String.format("R%s:B%s - ", padNum(repoNum), padNum(branchNum));

        log(Level.INFO, prefix + message);
    }

    @Override
    public void logTagMsg(String message, int repoNum, int tagNum) {
        String prefix = String.format("R%s:T%s - ", padNum(repoNum), padNum(tagNum));

        log(Level.INFO, prefix + message);
    }


    private String padNum(int number) {
        return String.format("%03d", number);
    }

}
