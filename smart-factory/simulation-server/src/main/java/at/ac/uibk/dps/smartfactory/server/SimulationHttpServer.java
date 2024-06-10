package at.ac.uibk.dps.smartfactory.server;

import at.ac.uibk.dps.smartfactory.SimpleLogger;
import at.ac.uibk.dps.smartfactory.objects.ContextVariable;
import at.ac.uibk.dps.smartfactory.objects.Response;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.ByteString;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
  private final boolean useProto;

  public SimulationHttpServer(Map<String, Response> pathToResponseMap, int port, boolean useProto) throws IOException {
    this.pathToResponseMap = pathToResponseMap;
    this.server = HttpServer.create(new InetSocketAddress(port), 0);
    this.useProto = useProto;
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
   * Converts a stream into a {@link ContextVariableProtos.ValueCollection} proto message.
   *
   * @param stream stream to convert.
   * @return {@link ContextVariableProtos.ValueCollection} proto message.
   */
  private static ContextVariableProtos.ValueCollection toCollectionProto(Stream<?> stream) {
    return ContextVariableProtos.ValueCollection.newBuilder()
        .addAllEntry(stream
            .map(SimulationHttpServer::toProto)
            .collect(Collectors.toList()))
        .build();
  }

  /**
   * Converts a map into a {@link ContextVariableProtos.ValueMap} proto message.
   *
   * @param map map to convert.
   * @return {@link ContextVariableProtos.ValueMap} proto message.
   */
  private static ContextVariableProtos.ValueMap toMapProto(Map<?, ?> map) {
    return ContextVariableProtos.ValueMap.newBuilder()
        .addAllEntry(map.entrySet().stream()
            .map(entry -> ContextVariableProtos.ValueMapEntry.newBuilder()
                .setKey(toProto(entry.getKey()))
                .setValue(toProto(entry.getValue()))
                .build())
            .collect(Collectors.toList()))
        .build();
  }

  /**
   * Returns a proto for the given value.
   *
   * @return Proto.
   * @throws UnsupportedOperationException If the value type is unknown.
   */
  public static ContextVariableProtos.Value toProto(Object value) throws UnsupportedOperationException {
    final var builder = ContextVariableProtos.Value.newBuilder();

    switch (value) {
      case Integer i -> builder.setInteger(i);
      case Float f -> builder.setFloat(f);
      case Long l -> builder.setLong(l);
      case Double d -> builder.setDouble(d);
      case String s -> builder.setString(s);
      case Boolean b -> builder.setBool(b);
      case byte[] bytes -> builder.setBytes(ByteString.copyFrom(bytes));
      case Object[] array -> builder.setArray(toCollectionProto(Arrays.stream(array)));
      case List<?> list -> builder.setList(toCollectionProto(list.stream()));
      case Map<?, ?> map -> builder.setMap(toMapProto(map));
      default -> throw new UnsupportedOperationException("Value type could not be converted to proto");
    }

    return builder.build();
  }

  /**
   * Run this server
   */
  @Override
  public void run() {
    // Create endpoints
    pathToResponseMap.forEach(
        (path, responseData) -> server.createContext("/" + path, new ProtoHandler(path, responseData, useProto))
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
  private record ProtoHandler(String path, Response responseData, boolean useProto) implements HttpHandler {

    /**
     * Handles a http exchange
     *
     * @param exchange the exchange containing the request from the client
     * @throws IOException if an error occurs during handling the request
     */
    @Override
    public void handle(HttpExchange exchange) throws IOException {
      try {
        LOGGER.info(String.format("Handle request: %s", path));

        final var payload = exchange.getRequestBody().readAllBytes();

        // Deserialize the payload (must be Map<?, ?>)
        Map<?, ?> in;
        if (payload.length > 0) {
          if (useProto) {
            in = ContextVariableProtos.ContextVariables.parseFrom(payload)
                .getDataList().stream()
                .collect(Collectors.toMap(
                    ContextVariableProtos.ContextVariable::getName,
                    ContextVariableProtos.ContextVariable::getValue
                ));
          } else {
            in = new ObjectMapper().readValue(new String(payload), Map.class);
            if (in.containsKey("Parameter")) {
              in = (Map<?, ?>) in.get("Parameter");
            }
          }
        } else {
          in = new HashMap<>();
        }

        if (!in.isEmpty()) {
          final String inString = mapToString(in);
          LOGGER.info("Input: " + inString);
        }

        // Call handler and convert response context variables into a map
        final var responseBody = new ArrayList<>(responseData.handler().onHandle(in));

        /* TODO ENABLE OR REMOVE DELAY?
        int delay = responseData.delay().get();
        if (delay > 0) {
          Thread.sleep(delay);
        }
        */

        final var responseMap = responseBody.stream()
            .collect(Collectors.toMap(ContextVariable::name, ContextVariable::value));

        if (!responseBody.isEmpty()) {
          final String outString = mapToString(responseMap);
          LOGGER.info("Output: " + outString);
        }

        final byte[] out;
        if (useProto) {
          out = ContextVariableProtos.ContextVariables.newBuilder()
              .addAllData(responseBody.stream()
                  .map(contextVariable -> ContextVariableProtos.ContextVariable.newBuilder()
                      .setName(contextVariable.name())
                      .setValue(toProto(contextVariable.value()))
                      .build())
                  .toList()
              )
              .build()
              .toByteArray();
        }
        else {
          out = new ObjectMapper().writeValueAsBytes(responseMap);
        }

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

