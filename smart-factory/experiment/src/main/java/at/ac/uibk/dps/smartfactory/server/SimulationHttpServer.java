package at.ac.uibk.dps.smartfactory.server;

import at.ac.uibk.dps.cirrina.core.object.context.ContextVariable;
import at.ac.uibk.dps.smartfactory.TestLogger;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import io.fury.Fury;
import io.fury.config.Language;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class SimulationHttpServer implements Runnable {

  public static final Logger LOGGER = TestLogger.getLogger(SimulationHttpServer.class.getName());

  /**
   * Maps endpoint paths to suppliers of context variables, which are sent as responses
   */
  private final Map<String, Endpoint> pathToEndpointMap;

  private final HttpServer server;

  public SimulationHttpServer(Map<String, Endpoint> pathToEndpointMap, int port) throws IOException {
    this.pathToEndpointMap = pathToEndpointMap;
    this.server = HttpServer.create(new InetSocketAddress(port), 0);
  }

  private static String mapToString(Map<?,?> map) {
    return map.entrySet().stream()
        .map(entry -> String.format("%s: %s", entry.getKey(), entry.getValue()))
        .collect(Collectors.joining(", "));
  }

  @Override
  public void run() {
    // Create endpoints
    pathToEndpointMap.forEach(
        (path, endpoint) -> server.createContext("/" + path, new TestHandler(path, endpoint))
    );

    // Start the server
    server.start();
  }

  private record TestHandler(String path, Endpoint endpoint) implements HttpHandler {

    @Override
    public void handle(HttpExchange exchange) throws IOException {

      final var fury = Fury.builder()
          .withLanguage(Language.XLANG)
          .requireClassRegistration(false)
          .suppressClassRegistrationWarnings(true)
          .build();

      try {

        LOGGER.info(String.format("Handle request: %s", path));

        final var payload = exchange.getRequestBody().readAllBytes();

        // Deserialize the payload (must be Map<?, ?>)
        final var in = fury.deserialize(payload);
        assert in instanceof Map<?, ?>;

        if (!((Map<?, ?>) in).isEmpty()) {
          final String inString = mapToString((Map<?, ?>) in);
          LOGGER.info("Input: " + inString);
        }

        // Call handler and convert response context variables into a map
        Map<?, ?> responseBody = endpoint.handler().onHandle((Map<?, ?>) in).stream()
            .collect(Collectors.toMap(ContextVariable::name, ContextVariable::value));

        int delay = endpoint.delay().get();
        if (delay > 0) {
          Thread.sleep(delay);
        }

        if (!responseBody.isEmpty()) {
          final String outString = mapToString(responseBody);
          LOGGER.info("Output: " + outString);
        }

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

