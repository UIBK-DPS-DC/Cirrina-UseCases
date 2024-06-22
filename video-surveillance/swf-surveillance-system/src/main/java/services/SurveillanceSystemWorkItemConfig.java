package services;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.kie.kogito.process.impl.DefaultWorkItemHandlerConfig;

@ApplicationScoped
public class SurveillanceSystemWorkItemConfig extends DefaultWorkItemHandlerConfig {

  @Inject
  public SurveillanceSystemWorkItemConfig(
      @ConfigProperty(name = "serviceCameraCapture.url") 
      String serviceCameraCaptureUrl
  ) {
    register("services_cameraCapture", 
      new HttpWorkItemHandler(serviceCameraCaptureUrl));
  }
}
