package at.ac.uibk.dps.smartfactory.objects;

import java.util.List;
import java.util.function.Supplier;

/**
 * Response which produces a result.
 *
 * @param delayMs Delay in ms before the response is sent.
 * @param handler Result supplier.
 */
public record Response(Supplier<Integer> delayMs, ResponseHandler handler) {

  public static class Builder {

    private ResponseHandler handler;
    private Supplier<Integer> delay = () -> 0;

    public Builder dynamicResult(ResponseHandler onHandle) {
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

    public Response build() {
      assert handler != null;
      return new Response(delay, handler);
    }
  }
}
