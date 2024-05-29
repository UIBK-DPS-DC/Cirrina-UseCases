package at.ac.uibk.dps.cirrina.example.surveillance;


import at.ac.uibk.dps.cirrina.example.surveillance.utils.IoUtils;
import at.ac.uibk.dps.cirrina.execution.object.context.InMemoryContext;
import at.ac.uibk.dps.cirrina.execution.object.event.Event;
import at.ac.uibk.dps.cirrina.execution.object.event.EventHandler;
import at.ac.uibk.dps.cirrina.execution.service.ServiceImplementationBuilder;
import at.ac.uibk.dps.cirrina.execution.service.ServiceImplementationSelector;
import at.ac.uibk.dps.cirrina.execution.service.description.HttpServiceImplementationDescription;
import at.ac.uibk.dps.cirrina.execution.service.description.HttpServiceImplementationDescription.Method;
import at.ac.uibk.dps.cirrina.execution.service.description.ServiceImplementationDescription;
import at.ac.uibk.dps.cirrina.execution.service.description.ServiceImplementationType;
import at.ac.uibk.dps.cirrina.runtime.OfflineRuntime;
import java.io.IOException;

public class SurveillanceSystemUseCase {


  public static void main(String[] args)
      throws IOException, RuntimeException {
    // Parse the collaborative state machine description
    var collaborativeStateMachineClass = IoUtils.getCollaborativeStateMachineClass();

    // Create the offline runtime
    var runtime = getOfflineRuntime();

    // Specify the service implementations
    ServiceImplementationDescription[] serviceDescriptions = new ServiceImplementationDescription[3];
    {
      var service = new HttpServiceImplementationDescription();
      service.name = "camera.capture";
      service.type = ServiceImplementationType.HTTP;
      service.cost = 1.0f;
      service.local = true;
      service.scheme = "http";
      service.host = "localhost";
      service.port = 8001;
      service.endPoint = "/capture";
      service.method = Method.POST;

      serviceDescriptions[0] = service;
    }
    {
      var service = new HttpServiceImplementationDescription();
      service.name = "personDetection.detect";
      service.type = ServiceImplementationType.HTTP;
      service.cost = 1.0f;
      service.local = true;
      service.scheme = "http";
      service.host = "localhost";
      service.port = 8000;
      service.endPoint = "/process";
      service.method = Method.POST;

      serviceDescriptions[1] = service;
    }
    {
      // TODO: use lambda
      var service = new HttpServiceImplementationDescription();
      service.name = "faceDetection.detect";
      service.type = ServiceImplementationType.HTTP;
      service.cost = 1.0f;
      service.local = true;
      service.scheme = "http";
      service.host = "localhost";
      service.port = 8000;
      service.endPoint = "/process";
      service.method = Method.POST;

      serviceDescriptions[2] = service;
    }


    final var services = ServiceImplementationBuilder.from(serviceDescriptions).build();
    final var serviceImplementationSelector = new ServiceImplementationSelector(services);

    // Create the collaborative state machine instances
    final var instances = runtime.newInstance(collaborativeStateMachineClass, serviceImplementationSelector);
  }

  private static OfflineRuntime getOfflineRuntime() {
    // Mock an event handler
    var mockEventHandler = new EventHandler() {
      @Override
      public void close() throws Exception {
      }

      @Override
      public void sendEvent(Event event, String source) {
        propagateEvent(event);
      }

      @Override
      public void subscribe(String topic) {

      }

      @Override
      public void unsubscribe(String subject) {

      }

      @Override
      public void subscribe(String source, String subject) {
        // TODO: check if external events can be subscribed here
      }

      @Override
      public void unsubscribe(String source, String subject) {

      }

    };

    // Mock a persistent context using an in-memory context
    var mockPersistentContext = new InMemoryContext();

    // Create a runtime
    return new OfflineRuntime(mockEventHandler, mockPersistentContext);
  }
}
