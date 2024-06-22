package at.ac.uibk.dps.smartfactory;

import at.ac.uibk.dps.smartfactory.object.error.ErrorStrategy;
import at.ac.uibk.dps.smartfactory.server.SmartFactoryHttpServer;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;

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
    ServerArgs serverArgs = new ServerArgs();
    JCommander jCommander = JCommander.newBuilder()
        .addObject(serverArgs)
        .build();

    try {
      jCommander.parse(args);
    } catch (ParameterException e) {
      LOGGER.info("Invalid parameters: " + e.getMessage());
      jCommander.usage();
      System.exit(1);
    }

    LOGGER.info("Arguments: %s".formatted(serverArgs.toString()));
    LOGGER.info("Starting Server...");
    Thread httpServerThread = SmartFactoryHttpServer.runServer(serverArgs);
    httpServerThread.join();
  }

  public static final class ServerArgs {

    @Parameter(names = {"--port", "-p"}, description = "Port number")
    private Integer port = 0;

    @Parameter(names = {"--useProto", "-up"}, description = "Use protobuf for variables", arity = 1)
    private Boolean useProto = true;

    @Parameter(names = {"--useDelays", "-ud"}, description = "Cause response delays", arity = 1)
    private Boolean useDelays = false;

    @Parameter(names = {"--errorStrategy", "-e"}, description = "Error strategy used for scan, pickup and assemble results")
    private ErrorStrategy errorStrategy = ErrorStrategy.NO_ERRORS;

    public int getPort() {
      return port;
    }

    public boolean getUseProto() {
      return useProto;
    }

    public boolean getUseDelays() {
      return useDelays;
    }

    public ErrorStrategy getErrorStrategy() {
      return errorStrategy;
    }

    @Override
    public String toString() {
      return "PORT: %s, PROTO: %s, DELAYS: %s, ERROR_STRATEGY: %s"
          .formatted(
              port == 0 ? "DEFAULT" : port.toString(),
              useProto.toString(),
              useDelays.toString(),
              errorStrategy.toString()
          );
    }
  }
}
