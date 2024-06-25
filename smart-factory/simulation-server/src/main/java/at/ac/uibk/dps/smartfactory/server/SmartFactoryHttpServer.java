package at.ac.uibk.dps.smartfactory.server;

import at.ac.uibk.dps.smartfactory.Main;
import at.ac.uibk.dps.smartfactory.object.error.ErrorDeterminer;
import at.ac.uibk.dps.smartfactory.object.error.ErrorStrategy;
import at.ac.uibk.dps.smartfactory.object.response.Response;
import at.ac.uibk.dps.smartfactory.object.variable.ContextVariable;
import at.ac.uibk.dps.smartfactory.object.variable.DefaultVariableHandler;
import at.ac.uibk.dps.smartfactory.object.variable.ProtoVariableHandler;
import at.ac.uibk.dps.smartfactory.object.variable.VariableHandler;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.stream.Collectors;

/**
 * HTTP server for smart factory service simulation.
 * Initialized with various endpoints used in the smart factory use case.
 */
public class SmartFactoryHttpServer extends SimulationHttpServer {

  public static final int DEFAULT_PORT = 8000;

  private static final Random RANDOM = new Random(); //TODO Fixed seed for reproducibility?

  private static final BeltSensors SENSORS = new BeltSensors();

  private SmartFactoryHttpServer(int port, VariableHandler handler, boolean useDelays, ErrorStrategy errorStrategy)
      throws IOException {
    super(createPaths(errorStrategy), port, handler, useDelays);
  }

  /**
   * Run this HTTP server on the given port.
   *
   * @param args The arguments used to configure the server.
   * @return The HTTP server thread.
   * @throws IOException if the server could not be created.
   */
  public static Thread runServer(Main.ServerArgs args) throws IOException {
    final int actualPort = args.getPort() == 0
        ? DEFAULT_PORT
        : args.getPort();

    VariableHandler variableHandler;
    if (args.getUseProto()) {
      variableHandler = new ProtoVariableHandler();
    } else {
      variableHandler = new DefaultVariableHandler();
    }

    final var httpServer = new SmartFactoryHttpServer(
        actualPort, variableHandler, args.getUseDelays(), args.getErrorStrategy());

    final var httpServerThread = new Thread(httpServer);
    httpServerThread.start();

    LOGGER.info(String.format("Server started. Listening on port %d...", actualPort));
    return httpServerThread;
  }

  private static Map<String, Response> createPaths(ErrorStrategy errorStrategy) {
    final Map<String, Response> paths = new HashMap<>();

    final ErrorDeterminer errorDeterminerScan = errorStrategy.getErrorDeterminer();
    final ErrorDeterminer errorDeterminerPickup = ErrorStrategy.NO_ERRORS.getErrorDeterminer();
    final ErrorDeterminer errorDeterminerAssemble = ErrorStrategy.NO_ERRORS.getErrorDeterminer();

    paths.put(
        "beamDetectionStart",
        new Response.Builder()
            .dynamicResult(in -> List.of(var("isBeamInterrupted", SENSORS.detectAtStart())))
            .delay(() -> 100 + RANDOM.nextInt(50))
            .build()
    );

    paths.put(
        "beamDetectionEnd",
        new Response.Builder()
            .dynamicResult(in -> List.of(var("isBeamInterrupted", SENSORS.detectAtEnd())))
            .delay(() -> 100 + RANDOM.nextInt(50))
            .build()
    );

    paths.put(
        "takePhoto",
        new Response.Builder()
            .staticResult(List.of(var("photoPath", "photo.png")))
            .delay(() -> 500 + RANDOM.nextInt(500))
            .build()
    );

    paths.put(
        "scanPhoto",
        new Response.Builder()
            .dynamicResult(in -> List.of(var("validObject", !errorDeterminerScan.isError(RANDOM))))
            .delay(() -> 1000 + RANDOM.nextInt(500))
            .build()
    );

    paths.put(
        "moveBelt",
        new Response.Builder()
            .dynamicResult(in -> {
              // The conveyor belt starts to move -> detect the object at the end of the belt (allow pickup)
              SENSORS.setDetectAtEnd();
              return List.of();
            })
            .delay(() -> 50 + RANDOM.nextInt(50))
            .build()
    );

    paths.put(
        "stopBelt",
        new Response.Builder()
            .dynamicResult(in -> {
              // The conveyor belt stops -> detect the next object at the start of the belt
              SENSORS.setDetectAtStart();
              return List.of();
            })
            .delay(() -> 50 + RANDOM.nextInt(50))
            .build()
    );

    paths.put(
        "pickUp",
        new Response.Builder()
            .dynamicResult(in -> List.of(var("pickUpSuccess", !errorDeterminerPickup.isError(RANDOM))))
            .delay(() -> 700 + RANDOM.nextInt(300))
            .build()
    );

    paths.put(
        "assemble",
        new Response.Builder()
            .dynamicResult(in -> List.of(var("assembleSuccess", !errorDeterminerAssemble.isError(RANDOM))))
            .delay(() -> 1000 + RANDOM.nextInt(500))
            .build()
    );

    paths.put(
        "returnToStart",
        new Response.Builder()
            .emptyResult()
            .delay(() -> 300 + RANDOM.nextInt(150))
            .build()
    );

    paths.put(
        "sendSms",
        new Response.Builder()
            .emptyResult()
            .delay(() -> 50 + RANDOM.nextInt(50))
            .build()
    );

    paths.put(
        "sendMail",
        new Response.Builder()
            .emptyResult()
            .delay(() -> 50 + RANDOM.nextInt(50))
            .build()
    );

    paths.put(
        "sendStatistics",
        new Response.Builder()
            .dynamicResult(in -> {
                logData(in, errorDeterminerScan);
                return List.of();
            })
            .delay(() -> 50 + RANDOM.nextInt(50))
            .build()
    );

    return paths;
  }

  private static void logData(Map<?,?> in, ErrorDeterminer errorDeterminer) {
    final var tmpDir = System.getProperty("java.io.tmpdir");
    final var tmpFilePath = Paths.get(tmpDir, "simulation-log.csv");

    try {
      if (!Files.exists(tmpFilePath)) {
        Files.createFile(tmpFilePath);

        String header = listToCsvLine(in.keySet());
        header = "%s,%s,%s\n".formatted("timestamp", header, "errorChance");
        Files.writeString(tmpFilePath, header, StandardOpenOption.APPEND);
      }

      String line = listToCsvLine(in.values());
      line = String.format(
          Locale.US, // US to ensure "." is used for floats instead of ","
          "%d,%s,%.3f\n",
          System.currentTimeMillis(),
          line,
          errorDeterminer.errorChance()
      );
      Files.writeString(tmpFilePath, line, StandardOpenOption.APPEND);

      //LOGGER.info("Stored to log: %s".formatted(tmpFilePath.toString()));
    } catch (IOException e) {
      LOGGER.severe("Failed writing to log: %s".formatted(e.getMessage()));
    }
  }

  private static String listToCsvLine(Collection<?> values) {
    return values.stream()
        .filter(Objects::nonNull)
        .map(value -> value.toString().replace('\n', ' ').trim())
        .collect(Collectors.joining(","));
  }

  private static ContextVariable var(String name, Object value) {
    return new ContextVariable(name, value);
  }

  private static class BeltSensors {

    private boolean objectAtStart = true;
    private boolean objectAtEnd = false;

    boolean detectAtStart() {
      return objectAtStart;
    }

    boolean detectAtEnd() {
      return objectAtEnd;
    }

    void setDetectAtStart() {
      objectAtStart = true;
      objectAtEnd = false;
    }

    void setDetectAtEnd() {
      objectAtStart = false;
      objectAtEnd = true;
    }
  }
}
