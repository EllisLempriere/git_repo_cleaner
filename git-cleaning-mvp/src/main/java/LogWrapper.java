import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.logging.*;

// What happens if logger cannot be set up correctly?
public class LogWrapper implements ILogWrapper {

    private Logger logger = Logger.getLogger("TempLoggerName");

    public LogWrapper() {
        logger.setLevel(Level.FINE);

        DateFormat dateFormat = new SimpleDateFormat("ddMMyyyy");
        String logName = dateFormat.format(System.currentTimeMillis()) + "Log.log";

        try {
            Handler handler = new FileHandler(logName, 5000, 1);
            handler.setFormatter(new CustomFormatter());

            logger.setUseParentHandlers(false);
            logger.addHandler(handler);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void log(Level level, String message) {
        logger.log(level, message);
    }
}
