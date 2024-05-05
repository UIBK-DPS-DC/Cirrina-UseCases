package at.ac.uibk.dps.smartfactory;

import java.io.IOException;
import java.util.Optional;
import java.util.logging.Logger;

public class TestServer {

  public static final Logger LOGGER = TestLogger.getLogger(TestServer.class.getName());

  public static void main(String[] args) throws IOException, InterruptedException {

    Optional<Integer> port;
    if (args.length == 0) {

      port = Optional.empty();
    }
    else {
      try {

        port = Optional.of(Integer.parseInt(args[0]));
      } catch (NumberFormatException e) {

        LOGGER.info(String.format("Invalid port: %s", args[0]));
        System.exit(1);
        return;
      }
    }

    LOGGER.info("Starting Server...");
    Thread httpServerThread = SmartFactoryHttpServer.runServer(port);
    httpServerThread.join();
  }
}
