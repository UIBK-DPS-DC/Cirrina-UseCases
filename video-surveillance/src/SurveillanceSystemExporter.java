package at.ac.uibk.dps.cirrina.example.surveillance;

import at.ac.uibk.dps.cirrina.io.plantuml.CollaborativeStateMachineExporter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;

public class SurveillanceSystemExporter {

  public static void main(String[] args) throws IOException {
    var out = new StringWriter();
    var csm = at.ac.uibk.dps.cirrina.example.surveillance.utils.IoUtils.getCollaborativeStateMachineClass();

    CollaborativeStateMachineExporter.export(out, csm);

    var file = new File("surveillance-system.puml");
    try (var writer = new FileWriter(file)) {
      writer.write(out.toString());
    }
  }

}
