package at.ac.uibk.dps.smartfactory.object.variable;

import java.util.Map;

public interface VariableHandler {

  byte[] toBytes(Map<String, Object> variables);

  Map<?, ?> fromBytes(byte[] bytes);
}
