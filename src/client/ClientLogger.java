package client;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * The ClientLogger class provides logging functionality for client activities and errors.
 * It writes log messages to a specified file with timestamps.
 */
public class ClientLogger {

    private BufferedWriter writer;

    /**
     * Constructs a new ClientLogger object that writes to the specified log file.
     *
     * @param logFilePath the path of the log file to write to.
     */
    public ClientLogger(String logFilePath) {
        try {
            writer = Files.newBufferedWriter(Paths.get(logFilePath), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            System.out.println("Error Initializing Logger: " + e);
        }
    }

    /**
     * Logs an error message with a timestamp.
     *
     * @param error the error message to log.
     */
    public void logError(String error) {
        try {
            String timeStamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"));
            writer.write("Error - " + error + " - " + timeStamp);
            writer.newLine();
            writer.flush();  // Ensure the log is written immediately
        } catch (IOException e) {
            System.out.println("Error Writing Log for error: " + e);
        }
    }

    /**
     * Logs an activity message with a timestamp.
     *
     * @param activity the activity message to log.
     */
    public void logActivity(String activity) {
        try {
            String timeStamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"));
            writer.write("Activity - " + activity + " - " + timeStamp);
            writer.newLine();
            writer.flush();  // Ensure the log is written immediately
        } catch (IOException e) {
            System.out.println("Error Writing Log for activity: " + e);
        }
    }

    /**
     * Closes the logger, releasing any resources it holds.
     */
    public void close() {
        try {
            if (writer != null) {
                writer.close();
            }
        } catch (IOException e) {
            System.out.println("Error closing logger: " + e);
        }
    }
}
