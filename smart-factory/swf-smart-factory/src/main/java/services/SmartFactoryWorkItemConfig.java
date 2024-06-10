package services;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.kie.kogito.process.impl.DefaultWorkItemHandlerConfig;
import org.kie.kogito.serverless.workflow.openapi.OpenApiWorkItemHandler;

@ApplicationScoped
public class SmartFactoryWorkItemConfig extends DefaultWorkItemHandlerConfig {

  @Inject
  public SmartFactoryWorkItemConfig(
      @ConfigProperty(name = "servicePickUp.url") 
      String servicePickUpUrl,

      @ConfigProperty(name = "serviceAssemble.url") 
      String serviceAssembleUrl,

      @ConfigProperty(name = "serviceReturnToStart.url") 
      String serviceReturnToStartUrl,
      
      @ConfigProperty(name = "serviceBeamDetectionStart.url") 
      String serviceBeamDetectionStartUrl,

      @ConfigProperty(name = "serviceBeamDetectionEnd.url") 
      String serviceBeamDetectionEndUrl,

      @ConfigProperty(name = "serviceTakePhoto.url") 
      String serviceTakePhotoUrl,

      @ConfigProperty(name = "serviceScanPhoto.url") 
      String serviceScanPhotoUrl,

      @ConfigProperty(name = "serviceMoveBelt.url")
      String serviceMoveBeltUrl,

      @ConfigProperty(name = "serviceStopBelt.url") 
      String serviceStopBeltUrl
  ) {
    register("services_servicePickUp", 
      new HttpWorkItemHandler(servicePickUpUrl));

    register("services_serviceAssemble", 
      new HttpWorkItemHandler(serviceAssembleUrl));

    register("services_serviceReturnToStart", 
      new HttpWorkItemHandler(serviceReturnToStartUrl));

    register("services_serviceBeamDetectionStart", 
      new HttpWorkItemHandler(serviceBeamDetectionStartUrl));

    register("services_serviceBeamDetectionEnd", 
      new HttpWorkItemHandler(serviceBeamDetectionEndUrl));

    register("services_serviceTakePhoto", 
      new HttpWorkItemHandler(serviceTakePhotoUrl));

    register("services_serviceScanPhoto", 
      new HttpWorkItemHandler(serviceScanPhotoUrl));
      
    register("services_serviceMoveBelt", 
      new HttpWorkItemHandler(serviceMoveBeltUrl));

    register("services_serviceStopBelt", 
      new HttpWorkItemHandler(serviceStopBeltUrl));
  }
}
