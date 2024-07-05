package at.ac.uibk.dps.smartfactory.server;

import at.ac.uibk.dps.smartfactory.SimpleLogger;
import at.ac.uibk.dps.smartfactory.object.variable.ContextVariable;
import at.ac.uibk.dps.smartfactory.object.response.Response;
import at.ac.uibk.dps.smartfactory.object.variable.VariableHandler;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * HTTP server for use case service simulation.
 */
public class SimulationHttpServer implements Runnable {

  public static final Logger LOGGER = SimpleLogger.getLogger(SimulationHttpServer.class.getName());

  /**
   * Maps responseData paths to endpoints (suppliers of context variables), which are sent as responses
   */
  private final Map<String, Response> pathToResponseMap;

  private final HttpServer server;
  private final VariableHandler variableHandler;
  private final boolean useDelays;

  public SimulationHttpServer(
      Map<String, Response> pathToResponseMap,
      int port,
      VariableHandler variableHandler,
      boolean useDelays
  ) throws IOException {
    this.pathToResponseMap = pathToResponseMap;
    this.server = HttpServer.create(new InetSocketAddress(port), 0);
    this.variableHandler = variableHandler;
    this.useDelays = useDelays;
  }

  /**
   * Map to string helper.
   *
   * @param map Map to convert.
   * @return The map as string.
   */
  private static String mapToString(Map<?,?> map) {
    return map.entrySet().stream()
        .map(entry -> String.format("%s: %s", entry.getKey(), entry.getValue()))
        .collect(Collectors.joining(", "));
  }

  /**
   * Run this server
   */
  @Override
  public void run() {
    // Create endpoints
    pathToResponseMap.forEach(
        (path, responseData) -> server.createContext(
            "/" + path,
            new Handler(path, responseData, variableHandler, useDelays)
        )
    );

    // Start the server
    server.start();
  }

  /**
   * HTTP handler for processing requests and generating responses.
   * Deserializes the request as proto and serializes the response as proto.
   *
   * @param path The path for which this handler is responsible (Only used for logging)
   * @param responseData The responseData to handle the request.
   */
  private record Handler(String path, Response responseData, VariableHandler variableHandler, boolean useDelays) implements HttpHandler {

    /**
     * Handles a http exchange
     *
     * @param exchange the exchange containing the request from the client
     * @throws IOException if an error occurs during handling the request
     * TODO currently doesn't care about the HTTP method for simplicity reasons
     */
    @Override
    public void handle(HttpExchange exchange) throws IOException {
      try {
        final var payload = exchange.getRequestBody().readAllBytes();

        // Deserialize the payload (must be Map<?, ?>)
        Map<?, ?> in;
        if (payload.length > 0) {
          in = variableHandler.fromBytes(payload);
        } else {
          in = new HashMap<>();
        }

        /*
        if (!in.isEmpty()) {
          final String inString = mapToString(in);
          //LOGGER.info("Input: " + inString);
        }
        */

        // Call handler and convert response context variables into a map
        final var responseBody = new ArrayList<>(responseData.handler().onHandle(in));

        // If delays are enabled, sleep for an amount of ms defined by the response handler
        if (useDelays) {
          int delay = responseData.delayMs().get();
          if (delay > 0) {
            Thread.sleep(delay);
          }
        }

        final var responseMap = responseBody.stream()
            .collect(Collectors.toMap(ContextVariable::name, ContextVariable::value));

        /*
        if (!responseBody.isEmpty()) {
          final String outString = mapToString(responseMap);
          LOGGER.info("Output: " + outString);
        }
        */

        final byte[] out = variableHandler.toBytes(responseMap);

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

        final var out = errorMessage.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(HttpURLConnection.HTTP_INTERNAL_ERROR, out.length);
        try (final var stream = exchange.getResponseBody()) {
          stream.write(out);
        }
      }
    }
  }
}

