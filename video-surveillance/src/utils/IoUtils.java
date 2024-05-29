package at.ac.uibk.dps.cirrina.example.surveillance.utils;


import at.ac.uibk.dps.cirrina.classes.collaborativestatemachine.CollaborativeStateMachineClass;
import at.ac.uibk.dps.cirrina.classes.collaborativestatemachine.CollaborativeStateMachineClassBuilder;
import at.ac.uibk.dps.cirrina.csml.description.CollaborativeStateMachineDescription;
import at.ac.uibk.dps.cirrina.example.surveillance.SurveillanceSystemUseCase;
import at.ac.uibk.dps.cirrina.io.description.DescriptionParser;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Objects;

public class IoUtils {

  private static final DescriptionParser<CollaborativeStateMachineDescription> parser = new DescriptionParser<>(
      CollaborativeStateMachineDescription.class);
  private static final String DESCRIPTION_FILE = "surveillance-system.csm";

  private IoUtils() {
  }

  public static CollaborativeStateMachineClass getCollaborativeStateMachineClass() throws IOException {
    try {
      ClassLoader classLoader = SurveillanceSystemUseCase.class.getClassLoader();
      File file = new File(Objects.requireNonNull(classLoader.getResource(DESCRIPTION_FILE)).getFile());
      var json = Files.readString(file.toPath());
      var csmDescription = parser.parse(json);
      return CollaborativeStateMachineClassBuilder.from(csmDescription).build();
    } catch (Exception e) {
      throw new IOException("Could not read description", e);
    }
  }

}
