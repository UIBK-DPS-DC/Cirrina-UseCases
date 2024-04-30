package at.ac.uibk.dps.cirrina.execution.command;

import at.ac.uibk.dps.cirrina.core.exception.CirrinaException;
import at.ac.uibk.dps.cirrina.core.object.action.InvokeAction;
import at.ac.uibk.dps.cirrina.core.object.context.Context;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

// TEMPORARY. SIMULATING SERVICE TYPE RESULTS.
public final class ActionInvokeCommand extends Command {

  private static Logger logger;

  private static boolean detectStart = true;
  private static boolean detectEnd = false;

  private final InvokeAction invokeAction;

  public ActionInvokeCommand(ExecutionContext executionContext, InvokeAction invokeAction) {
    super(executionContext);

    this.invokeAction = invokeAction;
  }

  public static void setLogger(Logger newLogger) {
    logger = newLogger;
  }

  // Simulate output for serviceType
  public static Object getValue(String serviceType) {
    return switch (serviceType) {
      case "beamDetectionStart" -> detectStart;
      case "beamDetectionEnd" -> detectEnd;
      case "takePhoto" -> "photo.png";
      case "scanPhoto", "pickUp", "assemble" -> Math.random() > 0.2F;
      default -> throw new IllegalStateException("Unexpected value: " + serviceType);
    };
  }

  @Override
  public void execute() throws CirrinaException {
    StringBuilder log = new StringBuilder(String.format("Invoke service: %s", invokeAction.getServiceType()));
    var extent = executionContext.status().getActivateState().getExtent();

    if (!invokeAction.getInput().isEmpty()) {
      String input = invokeAction.getInput().stream()
          .map(contextVariable -> {
            try {
              return String.format("%s = %s", contextVariable.name(), contextVariable.evaluate(extent).value());
            } catch (CirrinaException e) {
              throw new RuntimeException(e);
            }
          })
          .collect(Collectors.joining(", "));
      log.append(String.format(", Input: %s", input));
    }

    if (invokeAction.getOutput().isPresent()) {
      var value = getValue(invokeAction.getServiceType());
      log.append(String.format(", Output: %s", value.toString()));

      extent.trySet(
          invokeAction.getOutput().get().reference,
          value
      );

      log.append("\nVARS: ").append(extent.getContexts().stream().flatMap(context -> {
            try {
              return context.getAll().stream();
            } catch (CirrinaException e) {
              throw new RuntimeException(e);
            }
          })
          .map(variable -> String.format("%s = %s", variable.name(), variable.value()))
          .collect(Collectors.joining(", ")));
    }

    if (logger != null) {
      logger.info(log.toString());
    } else {
      System.out.println(log);
    }

    if (invokeAction.getServiceType().contains("moveBelt")) {
      detectStart = false;
      detectEnd = true;
    }
    if (invokeAction.getServiceType().contains("stopBelt")) {
      detectStart = true;
      detectEnd = false;
    }
  }
}
