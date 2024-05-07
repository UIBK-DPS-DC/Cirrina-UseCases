package at.ac.uibk.dps.smartfactory.server;

import at.ac.uibk.dps.cirrina.core.object.context.ContextVariable;

import java.util.List;
import java.util.function.Supplier;

public record Endpoint(Supplier<Integer> delay, EndpointHandler handler) {

  public static class Builder {

    private EndpointHandler handler;
    private Supplier<Integer> delay = () -> 0;

    public Builder dynamicResult(EndpointHandler onHandle) {
      this.handler = onHandle;
      return this;
    }

    public Builder staticResult(List<ContextVariable> resultList) {
      this.handler = in -> resultList;
      return this;
    }

    public Builder emptyResult() {
      this.handler = in -> List.of();
      return this;
    }

    public Builder delay(Supplier<Integer> delay) {
      this.delay = delay;
      return this;
    }

    public Endpoint build() {
      assert handler != null;
      return new Endpoint(delay, handler);
    }
  }
}
