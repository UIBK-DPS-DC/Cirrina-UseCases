package at.ac.uibk.dps.smartfactory;

import at.ac.uibk.dps.cirrina.classes.collaborativestatemachine.CollaborativeStateMachineClass;
import at.ac.uibk.dps.cirrina.classes.collaborativestatemachine.CollaborativeStateMachineClassBuilder;
import at.ac.uibk.dps.cirrina.csml.description.CollaborativeStateMachineDescription;
import at.ac.uibk.dps.cirrina.execution.object.context.Context;
import at.ac.uibk.dps.cirrina.execution.object.context.ContextBuilder;
import at.ac.uibk.dps.cirrina.execution.object.event.Event;
import at.ac.uibk.dps.cirrina.execution.object.event.NatsEventHandler;
import at.ac.uibk.dps.cirrina.execution.object.statemachine.StateMachine;
import at.ac.uibk.dps.cirrina.execution.service.ServiceImplementation;
import at.ac.uibk.dps.cirrina.execution.service.ServiceImplementationBuilder;
import at.ac.uibk.dps.cirrina.execution.service.ServiceImplementationSelector;
import at.ac.uibk.dps.cirrina.execution.service.description.HttpServiceImplementationDescription;
import at.ac.uibk.dps.cirrina.execution.service.description.ServiceImplementationType;
import at.ac.uibk.dps.cirrina.io.description.DescriptionParser;
import at.ac.uibk.dps.cirrina.runtime.OfflineRuntime;
import at.ac.uibk.dps.smartfactory.server.SmartFactoryHttpServer;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Runs a cirrina shared runtime with the use case and a
 */
public class TestLocal {

    public static final Logger LOGGER = TestLogger.getLogger(TestLocal.class.getName());

    public static void main(String[] args) {

        if (args.length == 0) {
            LOGGER.severe("Usage: TestLocal <csm_file_name>");
            System.exit(1);
        }

        // Read CSM file and construct parser
        String csmDescriptionString = null;
        try {
            csmDescriptionString = CsmHelper.readCsm(args[0]);
        } catch (FileNotFoundException e) {
            LOGGER.severe(String.format("CSM not found: %s%n%s", args[0], e.getMessage()));
            System.exit(1);
        }
        var parser = new DescriptionParser<>(CollaborativeStateMachineDescription.class);

        // Parse state machine
        CollaborativeStateMachineDescription csmDescription = null;
        try {
            csmDescription = parser.parse(csmDescriptionString);
        } catch (Exception e) {
            LOGGER.severe(String.format("Parse error: %s", e.getMessage()));
            System.exit(1);
        }

        LOGGER.info(String.format("CSM parsed: %s (%d state machines)", csmDescription.name, csmDescription.stateMachines.size()));

        // Check state machine
        CollaborativeStateMachineClass csmObject = null;
        try {
            csmObject = CollaborativeStateMachineClassBuilder.from(csmDescription).build();
        } catch (Exception e) {
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
            } catch (Exception e) {
                LOGGER.warning("Persistent context variables already exist or could not be created!");
            }

            // Create a shared runtime and run it
            Map<String, StateMachine> instances = new HashMap<>();
            var sharedRuntime = getSharedRuntime(persistentContext, natsServerURL, instances);

            var instanceIds = sharedRuntime.newInstance(csmObject, getServiceSelector());
            LOGGER.info(String.format("CSM instantiated: %d state machine instances.", instanceIds.size()));

            for (var instanceId : instanceIds) {
                var instance = sharedRuntime.findInstance(instanceId);
                if (instance.isPresent()) {
                    LOGGER.info(String.format("> %s", instance.get().getStateMachineObject().getName()));
                    instances.put(instanceId.toString(), instance.get());
                } else {
                    throw new IllegalStateException("Instance not found!");
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

    private static OfflineRuntime getSharedRuntime(Context persistentContext,
                                                   String natsServerURL, Map<String, StateMachine> instances) throws Exception {
        // Modified NATS Event handler with logging
        var eventHandler = new NatsEventHandler(natsServerURL) {
            @Override
            public void sendEvent(Event event, String source) throws IOException {
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

        // Subscribe to global.*
        eventHandler.subscribe(NatsEventHandler.GLOBAL_SOURCE, "*");

        return new OfflineRuntime(eventHandler, persistentContext);
    }
}
