package at.ac.uibk.dps.smartfactory.object.variable;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;

public class DefaultVariableHandler implements VariableHandler {

  @Override
  public byte[] toBytes(Map<String, Object> variables) {
    try {
      return new ObjectMapper().writeValueAsBytes(variables);
    } catch (JsonProcessingException e) {
      throw new RuntimeException("Could not convert output map: %s".formatted(e.getMessage()));
    }
  }

  @Override
  public Map<?, ?> fromBytes(byte[] bytes) {
    final Map<?, ?> in;
    try {
      in = new ObjectMapper().readValue(new String(bytes), Map.class);
    } catch (JsonProcessingException e) {
      throw new RuntimeException("Could not convert input bytes: %s".formatted(e.getMessage()));
    }

    // SonataFlow wraps input in a map under the key "Parameter"
    if (in.containsKey("Parameter")) {
      return (Map<?, ?>) in.get("Parameter");
    }
    return in;
  }
}
