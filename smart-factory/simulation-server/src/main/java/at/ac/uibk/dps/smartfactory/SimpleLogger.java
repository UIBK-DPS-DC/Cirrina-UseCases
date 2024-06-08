package at.ac.uibk.dps.smartfactory;

import java.util.Date;
import java.util.logging.*;

/**
 * Simple console based logger.
 */
public class SimpleLogger {

  public static Logger getLogger(String name) {
    var logger = Logger.getLogger(name);

    logger.setLevel(Level.INFO);
    logger.setUseParentHandlers(false);

    var consoleHandler = new ConsoleHandler();
    var formatter = new SimpleFormatter() {
      private static final String format = "[%1$tF %1$tT.%1$tL] [%2$-7s] %3$s %n";

      @Override
      public synchronized String format(LogRecord logRecord) {
        return String.format(format, new Date(logRecord.getMillis()), logRecord.getLevel().getLocalizedName(),
            logRecord.getMessage());
      }
    };
    consoleHandler.setFormatter(formatter);
    logger.addHandler(consoleHandler);

    return logger;
  }
}
