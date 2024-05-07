package at.ac.uibk.dps.smartfactory.server;

import at.ac.uibk.dps.cirrina.core.object.context.ContextVariable;

import java.util.List;
import java.util.Map;

@FunctionalInterface
public interface EndpointHandler {
  List<ContextVariable> onHandle(Map<?, ?> in);
}
