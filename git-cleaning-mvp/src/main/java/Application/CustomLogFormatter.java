package Application;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

public class CustomLogFormatter extends Formatter {

    private final DateFormat DATE_FORMAT = new SimpleDateFormat("hh:mm:ss.SSS");

    @Override
    public String format(LogRecord record) {
        StringBuilder sb = new StringBuilder();

        sb.append("[").append(DATE_FORMAT.format(new Date(record.getMillis()))).append("]");
        sb.append("-");
        sb.append("[").append(record.getLevel()).append("]");
        sb.append(" - ");
        sb.append(formatMessage(record));
        sb.append("\n");

        return sb.toString();
    }
}
