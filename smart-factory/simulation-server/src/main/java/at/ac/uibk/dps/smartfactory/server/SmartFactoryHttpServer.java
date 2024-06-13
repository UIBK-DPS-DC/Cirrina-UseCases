package at.ac.uibk.dps.smartfactory.server;

import at.ac.uibk.dps.smartfactory.object.response.Response;
import at.ac.uibk.dps.smartfactory.object.variable.ContextVariable;
import at.ac.uibk.dps.smartfactory.object.variable.DefaultVariableHandler;
import at.ac.uibk.dps.smartfactory.object.variable.ProtoVariableHandler;
import at.ac.uibk.dps.smartfactory.object.variable.VariableHandler;

import java.io.IOException;
import java.util.*;

/**
 * HTTP server for smart factory service simulation.
 * Initialized with various endpoints used in the smart factory use case.
 */
public class SmartFactoryHttpServer extends SimulationHttpServer {

  public static final int DEFAULT_PORT = 8000;

  private static final Random RANDOM = new Random(); //TODO Fixed seed for reproducibility?

  private static boolean detectAtStart = true;
  private static boolean detectAtEnd = true;

  public static final Map<String, Response> PATHS = new HashMap<>();
  static {
    PATHS.put(
        "beamDetectionStart",
        new Response.Builder()
            .dynamicResult(in -> List.of(var("isBeamInterrupted", detectAtStart)))
            .delay(() -> 100 + RANDOM.nextInt(50))
            .build()
    );

    PATHS.put(
        "beamDetectionEnd",
        new Response.Builder()
            .dynamicResult(in -> List.of(var("isBeamInterrupted", detectAtEnd)))
            .delay(() -> 100 + RANDOM.nextInt(50))
            .build()
    );

    PATHS.put(
        "takePhoto",
        new Response.Builder()
            .staticResult(List.of(var("photoPath", "photo.png")))
            .delay(() -> 500 + RANDOM.nextInt(500))
            .build()
    );

    PATHS.put(
        "scanPhoto",
        new Response.Builder()
            .dynamicResult(in -> List.of(var("validObject", RANDOM.nextFloat() > 0.1F)))
            .delay(() -> 1000 + RANDOM.nextInt(500))
            .build()
    );

    PATHS.put(
        "moveBelt",
        new Response.Builder()
            .dynamicResult(in -> {
              // The conveyor belt starts to move -> detect the object at the end of the belt (allow pickup)
              detectAtStart = false;
              detectAtEnd = true;
              return List.of();
            })
            .delay(() -> 50 + RANDOM.nextInt(50))
            .build()
    );

    PATHS.put(
        "stopBelt",
        new Response.Builder()
            .dynamicResult(in -> {
              // The conveyor belt stops -> detect the next object at the start of the belt
              detectAtStart = true;
              detectAtEnd = false;
              return List.of();
            })
            .delay(() -> 50 + RANDOM.nextInt(50))
            .build()
    );

    PATHS.put(
        "pickUp",
        new Response.Builder()
            .dynamicResult(in -> List.of(var("pickUpSuccess", RANDOM.nextFloat() > 0.1F)))
            .delay(() -> 700 + RANDOM.nextInt(300))
            .build()
    );

    PATHS.put(
        "assemble",
        new Response.Builder()
            .dynamicResult(in -> List.of(var("assembleSuccess", RANDOM.nextFloat() > 0.1F)))
            .delay(() -> 1000 + RANDOM.nextInt(500))
            .build()
    );

    PATHS.put(
        "returnToStart",
        new Response.Builder()
            .emptyResult()
            .delay(() -> 300 + RANDOM.nextInt(150))
            .build()
    );

    PATHS.put(
        "sendSms",
        new Response.Builder()
            .emptyResult()
            .delay(() -> 50 + RANDOM.nextInt(50))
            .build()
    );

    PATHS.put(
        "sendMail",
        new Response.Builder()
            .emptyResult()
            .delay(() -> 50 + RANDOM.nextInt(50))
            .build()
    );

    PATHS.put(
        "sendStatistics",
        new Response.Builder()
            .emptyResult()
            .delay(() -> 50 + RANDOM.nextInt(50))
            .build()
    );
  }

  private SmartFactoryHttpServer(int port, VariableHandler handler) throws IOException {
    super(PATHS, port, handler);
  }

  /**
   * Run this HTTP server on the given port.
   *
   * @param port The port the server will listen to.
   * @return The HTTP server thread.
   * @throws IOException if the server could not be created.
   */
  public static Thread runServer(int port, boolean useProto) throws IOException {
    final int actualPort = port == 0
        ? DEFAULT_PORT
        : port;

    VariableHandler variableHandler;
    if (useProto) {
      variableHandler = new ProtoVariableHandler();
    } else {
      variableHandler = new DefaultVariableHandler();
    }

    final var httpServer = new SmartFactoryHttpServer(actualPort, variableHandler);

    final var httpServerThread = new Thread(httpServer);
    httpServerThread.start();

    LOGGER.info(String.format("Server started. Listening on port %d...", actualPort));
    return httpServerThread;
  }

  private static ContextVariable var(String name, Object value) {
    return new ContextVariable(name, value);
  }
}
