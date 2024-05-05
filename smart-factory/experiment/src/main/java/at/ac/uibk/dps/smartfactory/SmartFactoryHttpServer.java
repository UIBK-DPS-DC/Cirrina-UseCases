package at.ac.uibk.dps.smartfactory;

import at.ac.uibk.dps.cirrina.core.object.context.ContextVariable;
import at.ac.uibk.dps.cirrina.core.object.context.ContextVariableBuilder;

import java.io.IOException;
import java.util.*;
import java.util.function.Function;

public class SmartFactoryHttpServer extends SimulationHttpServer {

  public static final int DEFAULT_PORT = 8000;

  private static final Random RANDOM = new Random(); //TODO Fixed seed for reproducibility?

  private static boolean detectAtStart = true;
  private static boolean detectAtEnd = false;

  public static final Map<String, Function<Map<?, ?>, List<ContextVariable>>> PATHS = new HashMap<>();
  static {
    PATHS.put("beamDetectionStart", in -> List.of(createVar("isBeamInterrupted", detectAtStart)));
    PATHS.put("beamDetectionEnd", in -> List.of(createVar("isBeamInterrupted", detectAtEnd)));
    PATHS.put("takePhoto", in -> List.of(createVar("photoPath", "photo.png")));
    PATHS.put("scanPhoto", in -> List.of(createVar("validObject", RANDOM.nextFloat() > 0.2F)));
    PATHS.put("moveBelt", in -> {
      // The conveyor belt starts to move -> detect the object at the end of the belt (allow pickup)
      detectAtStart = false;
      detectAtEnd = true;
      return List.of();
    });
    PATHS.put("stopBelt", in -> {
      // The conveyor belt stops -> detect the next object at the start of the belt
      detectAtStart = true;
      detectAtEnd = false;
      return List.of();
    });
    PATHS.put("pickUp", in -> List.of(createVar("pickUpSuccess", RANDOM.nextFloat() > 0.2F)));
    PATHS.put("assemble", in -> List.of(createVar("assembleSuccess", RANDOM.nextFloat() > 0.2F)));
    PATHS.put("returnToStart", in -> List.of());
    PATHS.put("sendSms", in -> List.of());
    PATHS.put("sendMail", in -> List.of());
    PATHS.put("sendStatistics", in -> List.of());
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
