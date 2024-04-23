package at.ac.uibk.dps.smartfactory;

import at.ac.uibk.dps.cirrina.core.exception.ParserException;
import at.ac.uibk.dps.cirrina.core.exception.VerificationException;
import at.ac.uibk.dps.cirrina.core.lang.classes.CollaborativeStateMachineClass;
import at.ac.uibk.dps.cirrina.core.lang.parser.Parser;
import at.ac.uibk.dps.cirrina.core.object.collaborativestatemachine.CollaborativeStateMachine;
import at.ac.uibk.dps.cirrina.core.object.collaborativestatemachine.CollaborativeStateMachineBuilder;
import at.ac.uibk.dps.cirrina.core.object.context.Context;
import at.ac.uibk.dps.cirrina.core.object.context.ContextBuilder;
import at.ac.uibk.dps.cirrina.core.exception.RuntimeException;
import at.ac.uibk.dps.cirrina.core.object.event.Event;
import at.ac.uibk.dps.cirrina.core.object.event.EventHandler;
import at.ac.uibk.dps.cirrina.runtime.instance.StateMachineInstance;
import at.ac.uibk.dps.cirrina.runtime.scheduler.RoundRobinRuntimeScheduler;
import at.ac.uibk.dps.cirrina.runtime.shared.SharedRuntime;

import java.io.FileNotFoundException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.*;
import java.util.stream.Collectors;

public class TestLocal {

    private static final Logger logger = getLogger();

    public static void main(String[] args) {
        if (args.length == 0) {
            logger.severe("Usage: TestParser <csm_file_name>");
            System.exit(1);
        }

        // Read CSM file and construct parser
        String csmDescription = null;
        try {
            csmDescription = CsmHelper.readCsm(args[0]);
        } catch (FileNotFoundException e) {
            logger.severe(String.format("CSM not found: %s%n%s", args[0], e.getMessage()));
            System.exit(1);
        }
        Parser parser = new Parser(new Parser.Options());

        // Parse state machine
        CollaborativeStateMachineClass csmClass = null;
        try {
            csmClass = parser.parse(csmDescription);
        } catch (ParserException e) {
            logger.severe(String.format("Parse error: %s", e.getMessage()));
            System.exit(1);
        }

        logger.info(String.format("CSM parsed: %s (%d state machines)%n", csmClass.name, csmClass.stateMachines.size()));

        // Check state machine
        CollaborativeStateMachine csmObject = null;
        try {
            csmObject = CollaborativeStateMachineBuilder.from(csmClass).build();
        } catch (VerificationException e) {
            logger.severe(String.format("Check error: %s", e.getMessage()));
            System.exit(1);
        }

        logger.info(String.format("CSM built: %s (%d state machines)%n", csmObject, csmObject.vertexSet().size()));

        // Get the NATS URL
        String natsServerURL = System.getenv("NATS_SERVER_URL");
        assert natsServerURL != null;

        try (var persistentContext = ContextBuilder.from().natsContext(natsServerURL, "persistent").build()) {

            // Try add persistent context variables
            try {
                persistentContext.create("jobDone", false);
                persistentContext.create("productsCompleted", 0);
                persistentContext.create("log", new String[0]);
            }
            catch (RuntimeException e) {
                logger.warning("Persistent context variables already exist or could not be created");
            }

            // Create a shared runtime and run it
            Map<String, StateMachineInstance> instances = new HashMap<>();
            var sharedRuntime = getSharedRuntime(persistentContext, instances);
            var instanceIds = sharedRuntime.newInstance(csmObject);
            logger.info(String.format("CSM instantiated: %d state machine instances%n", instanceIds.size()));

            for (var instanceId : instanceIds) {
                var instance = sharedRuntime.findInstance(instanceId);
                if (instance.isPresent()) {
                    logger.info(String.format("> %s", instance.get().getStateMachine().getName()));
                    instances.put(instanceId.toString(), instance.get());
                }
                else {
                    logger.severe("Instance not found!");
                    System.exit(1);
                }
            }

            var thread = new Thread(sharedRuntime);
            thread.start();
            try {
                thread.join();
            }
            catch (InterruptedException e) {
                logger.info("Interrupted");
            }
        } catch (Exception e) {
          logger.severe(String.format("Runtime error: %s", e.getMessage()));
        }
    }

    private static SharedRuntime getSharedRuntime(Context persistentContext,
                                                  Map<String, StateMachineInstance> instances) throws Exception {
        return new SharedRuntime(new RoundRobinRuntimeScheduler(), new EventHandler() {

            @Override
            public void close() {
                logger.info("Closing");
            }

            @Override
            public void sendEvent(Event event, String source) {
                propagateEvent(event);

                logger.info(String.format("Send event '%s' (Source: %s)", event,
                    instances.get(source).getStateMachine().getName()));

                if (!event.getData().isEmpty()) {
                    logger.info(event.getData().stream()
                        .map(var -> var.name() + " = " + var.value())
                        .collect(Collectors.joining(", ")));
                }
            }

            @Override
            public void subscribe(String subject) {
                logger.info(String.format("Subscribe to: %s", subject));
            }

            @Override
            public void unsubscribe(String subject) {
                logger.info(String.format("Unsubscribe to: %s", subject));
            }

            @Override
            public void subscribe(String source, String subject) {
                logger.info(String.format("Subscribe to: %s (Source: %s)", subject,
                    instances.get(source).getStateMachine().getName()));
            }

            @Override
            public void unsubscribe(String source, String subject) {
                logger.info(String.format("Subscribe to: %s (Source: %s)", subject,
                    instances.get(source).getStateMachine().getName()));
            }
        }, persistentContext);
    }

    private static Logger getLogger() {
        var logger = Logger.getLogger(TestLocal.class.getName());

        logger.setLevel(Level.INFO);
        logger.setUseParentHandlers(false);

        var consoleHandler = new ConsoleHandler();
        var formatter = new SimpleFormatter() {
            private static final String format = "[%1$tF %1$tT.%1$tL] [%2$-7s] %3$s %n";

            @Override
            public synchronized String format(LogRecord logRecord) {
                return String.format(format, new Date(logRecord.getMillis()), logRecord.getLevel().getLocalizedName(),
                    logRecord.getMessage());
            }
        };
        consoleHandler.setFormatter(formatter);
        logger.addHandler(consoleHandler);

        return logger;
    }
}
