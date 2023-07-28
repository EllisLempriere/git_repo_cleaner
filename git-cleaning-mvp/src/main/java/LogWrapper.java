import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.logging.*;

// What happens if logger cannot be set up correctly?
// TODO - Improve all logging messages
public class LogWrapper implements ILogWrapper {

    private final Logger LOGGER = Logger.getLogger("TempLoggerName");

    public LogWrapper() {
        LOGGER.setLevel(Level.FINE);

        DateFormat dateFormat = new SimpleDateFormat("ddMMyyyy");
        String logName = dateFormat.format(System.currentTimeMillis()) + "Log.log";

        try {
            Handler handler = new FileHandler(logName, 5000, 1);
            handler.setFormatter(new CustomFormatter());

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
}
