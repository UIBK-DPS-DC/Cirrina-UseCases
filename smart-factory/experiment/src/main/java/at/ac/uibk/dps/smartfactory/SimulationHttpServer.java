package at.ac.uibk.dps.smartfactory;

import at.ac.uibk.dps.cirrina.core.object.context.ContextVariable;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import io.fury.Fury;
import io.fury.config.Language;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class SimulationHttpServer implements Runnable {

  public static final Logger LOGGER = TestLogger.getLogger(SimulationHttpServer.class.getName());

  /**
   * Maps endpoint paths to suppliers of context variables, which are sent as responses
   */
  private final Map<String, Function<Map<?, ?>, List<ContextVariable>>> pathToOutputMap;

  private final HttpServer server;

  public SimulationHttpServer(Map<String, Function<Map<?, ?>, List<ContextVariable>>> pathToOutputMap, int port) throws IOException {
    this.pathToOutputMap = pathToOutputMap;
    this.server = HttpServer.create(new InetSocketAddress(port), 0);
  }

  @Override
  public void run() {
    // Create endpoints
    pathToOutputMap.forEach(
        (path, variables) -> server.createContext("/" + path, new TestHandler(path, variables))
    );

    // Start the server
    server.start();
  }

  private record TestHandler(String path,
                             Function<Map<?, ?>, List<ContextVariable>> contextVariables) implements HttpHandler {

    @Override
      public void handle(HttpExchange exchange) throws IOException {

        final var fury = Fury.builder()
            .withLanguage(Language.XLANG)
            .requireClassRegistration(false)
            .build();

        try {

          LOGGER.info(String.format("Handle request: %s", path));

          final var payload = exchange.getRequestBody().readAllBytes();

          // Deserialize the payload (must be Map<?, ?>)
          final var in = fury.deserialize(payload);
          assert in instanceof Map<?, ?>;

          final String inString = ((Map<?, ?>) in).entrySet().stream()
              .map(entry -> String.format("%s: %s", entry.getKey(), entry.getValue()))
              .collect(Collectors.joining(", "));

          if (!inString.trim().isEmpty()) {
            LOGGER.info("Input: " + inString);
          }

          // Convert contextVariables to response map
          Map<?, ?> responseBody = contextVariables.apply((Map<?, ?>) in).stream()
              .collect(Collectors.toMap(ContextVariable::name, ContextVariable::value));

          final var out = fury.serialize(responseBody);

          // Response status and length
          exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, out.length);

          // Write response
          try (final var stream = exchange.getResponseBody()) {
            stream.write(out);
          }
        } catch (Exception e) {
          // Handle exceptions and send appropriate response
          String errorMessage = "Internal Server Error: " + e.getMessage();

          final var out = fury.serialize(errorMessage);

          exchange.sendResponseHeaders(HttpURLConnection.HTTP_INTERNAL_ERROR, out.length);

          try (final var stream = exchange.getResponseBody()) {
            stream.write(out);
          }
        }
      }
    }
}

