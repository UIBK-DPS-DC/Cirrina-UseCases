package at.ac.uibk.dps.smartfactory.object.variable;

import at.ac.uibk.dps.smartfactory.server.ContextVariableProtos;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ProtoVariableHandler implements VariableHandler {

  /**
   * Converts a stream into a {@link ContextVariableProtos.ValueCollection} proto message.
   *
   * @param stream stream to convert.
   * @return {@link ContextVariableProtos.ValueCollection} proto message.
   */
  private static ContextVariableProtos.ValueCollection toCollectionProto(Stream<?> stream) {
    return ContextVariableProtos.ValueCollection.newBuilder()
        .addAllEntry(stream
            .map(ProtoVariableHandler::toProto)
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

  @Override
  public byte[] toBytes(Map<String, Object> variables) {
    return ContextVariableProtos.ContextVariables.newBuilder()
        .addAllData(variables.entrySet().stream()
            .map(contextVariable -> ContextVariableProtos.ContextVariable.newBuilder()
                .setName(contextVariable.getKey())
                .setValue(toProto(contextVariable.getValue()))
                .build())
            .toList()
        )
        .build()
        .toByteArray();
  }

  @Override
  public Map<?, ?> fromBytes(byte[] bytes) {
    try {
      return ContextVariableProtos.ContextVariables.parseFrom(bytes)
          .getDataList().stream()
          .collect(Collectors.toMap(
              ContextVariableProtos.ContextVariable::getName,
              ContextVariableProtos.ContextVariable::getValue
          ));
    } catch (InvalidProtocolBufferException e) {
      throw new RuntimeException("Could not convert input bytes: %s".formatted(e.getMessage()));
    }
  }
}
