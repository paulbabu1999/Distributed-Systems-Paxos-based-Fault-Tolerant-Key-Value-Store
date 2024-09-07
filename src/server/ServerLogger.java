package server;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * The ServerLogger class provides methods for logging server activities and errors to a file.
 */
public class ServerLogger {

    private BufferedWriter writer;

    /**
     * Constructs a new ServerLogger with the specified log file path.
     *
     * @param logFilePath the path to the log file
     */
    public ServerLogger(String logFilePath) {
        try {
            writer = Files.newBufferedWriter(Paths.get(logFilePath), StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND);
        } catch (IOException e) {
            System.out.println("Error Initializing Logger: " + e);
        }
    }

    /**
     * Logs an error message along with a timestamp to the log file.
     *
     * @param error the error message to be logged
     */
    public void logError(String error) {
        try {
            String timeStamp = LocalDateTime.now()
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            writer.write("Error - " + error + " - " + timeStamp);
            writer.newLine();
            writer.flush();
        } catch (IOException e) {
            System.out.println("Error Writing Log for error: " + e);
        }
    }

    /**
     * Logs an activity message along with a timestamp to the log file.
     *
     * @param activity the activity message to be logged
     */
    public void logActivity(String activity) {
        try {
            String timeStamp = LocalDateTime.now()
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            writer.write("Activity - " + activity + " - " + timeStamp);
            writer.newLine();
            writer.flush();
        } catch (IOException e) {
            System.out.println("Error Writing Log for activity: " + e);
        }
    }
}
