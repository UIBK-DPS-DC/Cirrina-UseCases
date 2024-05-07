package at.ac.uibk.dps.smartfactory.server;

import at.ac.uibk.dps.cirrina.core.object.context.ContextVariable;
import at.ac.uibk.dps.cirrina.core.object.context.ContextVariableBuilder;

import java.io.IOException;
import java.util.*;

public class SmartFactoryHttpServer extends SimulationHttpServer {

  public static final int DEFAULT_PORT = 8000;

  private static final Random RANDOM = new Random(); //TODO Fixed seed for reproducibility?

  private static boolean detectAtStart = true;
  private static boolean detectAtEnd = true;

  public static final Map<String, Endpoint> PATHS = new HashMap<>();
  static {
    PATHS.put(
        "beamDetectionStart",
        new Endpoint.Builder()
            .dynamicResult(in -> List.of(createVar("isBeamInterrupted", detectAtStart)))
            .delay(() -> 100 + RANDOM.nextInt(50))
            .build()
    );

    PATHS.put(
        "beamDetectionEnd",
        new Endpoint.Builder()
            .dynamicResult(in -> List.of(createVar("isBeamInterrupted", detectAtEnd)))
            .delay(() -> 100 + RANDOM.nextInt(50))
            .build()
    );

    PATHS.put(
        "takePhoto",
        new Endpoint.Builder()
            .staticResult(List.of(createVar("photoPath", "photo.png")))
            .delay(() -> 500 + RANDOM.nextInt(500))
            .build()
    );

    PATHS.put(
        "scanPhoto",
        new Endpoint.Builder()
            .dynamicResult(in -> List.of(createVar("validObject", RANDOM.nextFloat() > 0.1F)))
            .delay(() -> 1000 + RANDOM.nextInt(500))
            .build()
    );

    PATHS.put(
        "moveBelt",
        new Endpoint.Builder()
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
        new Endpoint.Builder()
            .dynamicResult(in -> {
              // The conveyor belt stops -> detect the next object at the start of the belt
              detectAtStart = false;
              detectAtEnd = false;
              return List.of();
            })
            .delay(() -> 50 + RANDOM.nextInt(50))
            .build()
    );

    PATHS.put(
        "pickUp",
        new Endpoint.Builder()
            .dynamicResult(in -> {
              detectAtStart = true;
              return List.of(createVar("pickUpSuccess", RANDOM.nextFloat() > 0.1F));
            })
            .delay(() -> 700 + RANDOM.nextInt(300))
            .build()
    );

    PATHS.put(
        "assemble",
        new Endpoint.Builder()
            .dynamicResult(in -> List.of(createVar("assembleSuccess", RANDOM.nextFloat() > 0.1F)))
            .delay(() -> 1000 + RANDOM.nextInt(500))
            .build()
    );

    PATHS.put(
        "returnToStart",
        new Endpoint.Builder()
            .emptyResult()
            .delay(() -> 300 + RANDOM.nextInt(150))
            .build()
    );

    PATHS.put(
        "sendSms",
        new Endpoint.Builder()
            .emptyResult()
            .delay(() -> 50 + RANDOM.nextInt(50))
            .build()
    );

    PATHS.put(
        "sendMail",
        new Endpoint.Builder()
            .emptyResult()
            .delay(() -> 50 + RANDOM.nextInt(50))
            .build()
    );

    PATHS.put(
        "sendStatistics",
        new Endpoint.Builder()
            .emptyResult()
            .delay(() -> 50 + RANDOM.nextInt(50))
            .build()
    );
  }

  private SmartFactoryHttpServer(int port) throws IOException {
    super(PATHS, port);
  }

  public static Thread runServer(Optional<Integer> portOptional) throws IOException {
    int port = portOptional.orElse(DEFAULT_PORT);
    var httpServer = new SmartFactoryHttpServer(port);

    var httpServerThread = new Thread(httpServer);
    httpServerThread.start();

    LOGGER.info(String.format("Server started. Listening on port %d...", port));
    return httpServerThread;
  }

  private static ContextVariable createVar(String path, Object value) {
    return ContextVariableBuilder.from().name(path).value(value).build();
  }
}
