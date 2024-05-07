package at.ac.uibk.dps.smartfactory;

import at.ac.uibk.dps.cirrina.core.exception.CirrinaException;
import at.ac.uibk.dps.cirrina.core.exception.VerificationException;
import at.ac.uibk.dps.cirrina.core.lang.classes.CollaborativeStateMachineClass;
import at.ac.uibk.dps.cirrina.core.lang.parser.Parser;
import at.ac.uibk.dps.cirrina.core.object.collaborativestatemachine.CollaborativeStateMachine;
import at.ac.uibk.dps.cirrina.core.object.collaborativestatemachine.CollaborativeStateMachineBuilder;
import at.ac.uibk.dps.cirrina.core.object.context.*;
import at.ac.uibk.dps.cirrina.core.object.event.Event;
import at.ac.uibk.dps.cirrina.core.object.event.NatsEventHandler;
import at.ac.uibk.dps.cirrina.execution.instance.statemachine.StateMachineInstance;
import at.ac.uibk.dps.cirrina.execution.service.ServiceImplementation;
import at.ac.uibk.dps.cirrina.execution.service.ServiceImplementationBuilder;
import at.ac.uibk.dps.cirrina.execution.service.ServiceImplementationSelector;
import at.ac.uibk.dps.cirrina.execution.service.description.HttpServiceImplementationDescription;
import at.ac.uibk.dps.cirrina.execution.service.description.ServiceImplementationType;
import at.ac.uibk.dps.cirrina.runtime.SharedRuntime;
import at.ac.uibk.dps.cirrina.runtime.scheduler.RoundRobinRuntimeScheduler;
import at.ac.uibk.dps.smartfactory.server.SmartFactoryHttpServer;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;
import java.util.logging.*;
import java.util.stream.Collectors;

/**
 * Runs a cirrina shared runtime with the use case and a
 */
public class TestLocal {

    public static final Logger LOGGER = TestLogger.getLogger(TestLocal.class.getName());

    public static void main(String[] args) throws Exception {

        if (args.length == 0) {
            LOGGER.severe("Usage: TestLocal <csm_file_name>");
            System.exit(1);
        }

        // Read CSM file and construct parser
        String csmDescription = null;
        try {
            csmDescription = CsmHelper.readCsm(args[0]);
        } catch (FileNotFoundException e) {
            LOGGER.severe(String.format("CSM not found: %s%n%s", args[0], e.getMessage()));
            System.exit(1);
        }
        var parser = new Parser<>(CollaborativeStateMachineClass.class);

        // Parse state machine
        CollaborativeStateMachineClass csmClass = null;
        try {
            csmClass = parser.parse(csmDescription);
        } catch (CirrinaException e) {
            LOGGER.severe(String.format("Parse error: %s", e.getMessage()));
            System.exit(1);
        }

        LOGGER.info(String.format("CSM parsed: %s (%d state machines)", csmClass.name, csmClass.stateMachines.size()));

        // Check state machine
        CollaborativeStateMachine csmObject = null;
        try {
            csmObject = CollaborativeStateMachineBuilder.from(csmClass).build();
        } catch (VerificationException e) {
            LOGGER.severe(String.format("Check error: %s", e.getMessage()));
            System.exit(1);
        }

        LOGGER.info(String.format("CSM built: %s (%d state machines)", csmObject, csmObject.vertexSet().size()));

        Thread httpServerThread = null;
        try {
            httpServerThread = SmartFactoryHttpServer.runServer(Optional.empty());
            LOGGER.info(String.format("Listening for service invocations on port %d.", SmartFactoryHttpServer.DEFAULT_PORT));
        } catch (IOException e) {
            LOGGER.severe(String.format("Could not run HTTP Server: %s", e.getMessage()));
            System.exit(1);
        }

        // Get the NATS URL
        String natsServerURL = System.getenv("NATS_SERVER_URL");
        assert natsServerURL != null;

        try (var persistentContext = ContextBuilder.from().natsContext(natsServerURL, "persistent").build()) {

            // Try add persistent context variables
            try {
                persistentContext.create("jobDone", false);
                persistentContext.create("productsCompleted", 0);
                persistentContext.create("log", new ArrayList<>());
            } catch (CirrinaException e) {
                LOGGER.warning("Persistent context variables already exist or could not be created!");
            }

            // Create a shared runtime and run it
            Map<String, StateMachineInstance> instances = new HashMap<>();
            var sharedRuntime = getSharedRuntime(persistentContext, csmObject, natsServerURL, instances);
            var instanceIds = sharedRuntime.newInstance(csmObject, getServiceSelector());
            LOGGER.info(String.format("CSM instantiated: %d state machine instances.", instanceIds.size()));

            for (var instanceId : instanceIds) {
                var instance = sharedRuntime.findInstance(instanceId);
                if (instance.isPresent()) {
                    LOGGER.info(String.format("> %s", instance.get().getStateMachineObject().getName()));
                    instances.put(instanceId.toString(), instance.get());
                } else {
                    throw CirrinaException.from("Instance not found!");
                }
            }

            try {
                Thread.currentThread().join();
            } catch (InterruptedException e) {
                LOGGER.info("Interrupted");
            }
        } catch (Exception e) {
          LOGGER.severe(String.format("Runtime error: %s", e.getMessage()));
        }
        finally {
            LOGGER.info("Shutting down...");
            httpServerThread.interrupt();
        }
    }

    private static ServiceImplementationSelector getServiceSelector() {

        Multimap<String, ServiceImplementation> serviceImplementations
            = ArrayListMultimap.create(SmartFactoryHttpServer.PATHS.size(), 1);

        for (String path : SmartFactoryHttpServer.PATHS.keySet()) {

            var service = new HttpServiceImplementationDescription();
            service.name = path;
            service.type = ServiceImplementationType.HTTP;
            service.cost = 1.0f;
            service.local = true;
            service.scheme = "http";
            service.host = "localhost";
            service.port = SmartFactoryHttpServer.DEFAULT_PORT;
            service.method = HttpServiceImplementationDescription.Method.GET;
            service.endPoint = "/" + path;

            serviceImplementations.put(path, ServiceImplementationBuilder.from(service).build());
        }

        return new ServiceImplementationSelector(serviceImplementations);
    }

    private static SharedRuntime getSharedRuntime(Context persistentContext, CollaborativeStateMachine csm,
                                                  String natsServerURL, Map<String, StateMachineInstance> instances) throws Exception {
        var eventHandler = new NatsEventHandler(natsServerURL) {
            @Override
            public void sendEvent(Event event, String source) throws CirrinaException {
                super.sendEvent(event, source);

                LOGGER.info(String.format("Send event '%s' (Source: %s, Current state: %s)", event,
                    instances.get(source).getStateMachineObject().getName(),
                    "Status")); // instances.get(source).getStatus().getActivateState().getState().getName()

                if (!event.getData().isEmpty()) {
                    LOGGER.info(event.getData().stream()
                        .map(var -> var.name() + " = " + var.value())
                        .collect(Collectors.joining(", ")));
                }
            }
        };
        eventHandler.subscribe(NatsEventHandler.GLOBAL_SOURCE, "*");

        return new SharedRuntime(eventHandler, persistentContext);
    }
}
