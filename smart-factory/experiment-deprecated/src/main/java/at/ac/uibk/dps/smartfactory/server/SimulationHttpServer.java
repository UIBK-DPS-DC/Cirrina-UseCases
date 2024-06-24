package at.ac.uibk.dps.smartfactory.server;

import at.ac.uibk.dps.cirrina.execution.object.context.ContextVariable;
import at.ac.uibk.dps.cirrina.execution.object.exchange.ContextVariableExchange;
import at.ac.uibk.dps.cirrina.execution.object.exchange.ContextVariableProtos;
import at.ac.uibk.dps.cirrina.execution.object.exchange.ValueExchange;
import at.ac.uibk.dps.smartfactory.TestLogger;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
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

      try {

        LOGGER.info(String.format("Handle request: %s", path));

        final var payload = exchange.getRequestBody().readAllBytes();

        // Deserialize the payload (must be Map<?, ?>)
        Map<?, ?> in;
        if (payload.length > 0) {
          in = ContextVariableProtos.ContextVariables.parseFrom(payload)
                .getDataList().stream()
                  .collect(Collectors.toMap(
                      ContextVariableProtos.ContextVariable::getName,
                      ContextVariableProtos.ContextVariable::getValue
                  ));
        } else {
          in = new HashMap<>();
        }

        if (!in.isEmpty()) {
          final String inString = mapToString(in);
          LOGGER.info("Input: " + inString);
        }

        // Call handler and convert response context variables into a map
        var responseBody = new ArrayList<>(endpoint.handler().onHandle(in));

        /* TODO ENABLE
        int delay = endpoint.delay().get();
        if (delay > 0) {
          Thread.sleep(delay);
        }
        */

        if (!responseBody.isEmpty()) {
          final String outString = mapToString(responseBody.stream()
              .collect(Collectors.toMap(ContextVariable::name, ContextVariable::value)));
          LOGGER.info("Output: " + outString);
        }

        final byte[] out = ContextVariableProtos.ContextVariables.newBuilder()
            .addAllData(responseBody.stream()
                .map(contextVariable -> new ContextVariableExchange(contextVariable).toProto())
                .toList()
            )
            .build()
            .toByteArray();

        // Response status and length
        exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, out.length);

        // Write response
        try (final var stream = exchange.getResponseBody()) {
          stream.write(out);
        }
      } catch (Exception e) {
        // Handle exceptions and send appropriate response
        String errorMessage = "Internal Server Error: " + e.getMessage();
        LOGGER.severe(errorMessage);

        final var out = new ValueExchange(errorMessage).toBytes();

        exchange.sendResponseHeaders(HttpURLConnection.HTTP_INTERNAL_ERROR, out.length);

        try (final var stream = exchange.getResponseBody()) {
          stream.write(out);
        }
      }
    }
  }
}

