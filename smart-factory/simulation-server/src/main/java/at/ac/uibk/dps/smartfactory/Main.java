package at.ac.uibk.dps.smartfactory;

import at.ac.uibk.dps.smartfactory.server.SmartFactoryHttpServer;

import java.io.IOException;
import java.util.logging.Logger;

public class Main {

  public static final Logger LOGGER = SimpleLogger.getLogger(Main.class.getName());

  /**
   * Main entry point.
   *
   * @throws IOException if running the HTTP server produced an error.
   * @throws InterruptedException if the HTTP server was interrupted.
   */
  public static void main(String[] args) throws IOException, InterruptedException {
    int port;
    if (args.length == 0) {
      port = 0;
    } else {
      try {
        port = Integer.parseInt(args[0]);
      } catch (NumberFormatException e) {
        LOGGER.info(String.format("Invalid port: %s", args[0]));
        System.exit(1);
        return;
      }
    }

    boolean useProto;
    if (args.length <= 1) {
      useProto = true;
    } else {
      useProto = args[1].equals("true");
    }

    LOGGER.info("Starting Server...");
    Thread httpServerThread = SmartFactoryHttpServer.runServer(port, useProto);
    httpServerThread.join();
  }
}
