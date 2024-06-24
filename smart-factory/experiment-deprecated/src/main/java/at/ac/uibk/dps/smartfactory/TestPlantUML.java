package at.ac.uibk.dps.smartfactory;

import at.ac.uibk.dps.cirrina.classes.collaborativestatemachine.CollaborativeStateMachineClass;
import at.ac.uibk.dps.cirrina.classes.collaborativestatemachine.CollaborativeStateMachineClassBuilder;
import at.ac.uibk.dps.cirrina.csml.description.CollaborativeStateMachineDescription;
import at.ac.uibk.dps.cirrina.io.description.DescriptionParser;
import at.ac.uibk.dps.cirrina.io.plantuml.CollaborativeStateMachineExporter;

import java.io.File;
import java.io.FileWriter;
import java.io.StringWriter;
import java.util.logging.Logger;

public class TestPlantUML {

  private static final Logger logger = Logger.getLogger(TestPlantUML.class.getName());

  public static void main(String[] args) throws Exception {
    if (args.length == 0) {
      logger.severe("Usage: TestPlantUML <csm_file_name>");
      System.exit(1);
    }

    // Read CSM file and construct parser
    String csmDescriptionString = CsmHelper.readCsm(args[0]);
    var parser = new DescriptionParser<>(CollaborativeStateMachineDescription.class);

    // Parse state machine
    CollaborativeStateMachineDescription csmClass = parser.parse(csmDescriptionString);
    logger.info(String.format("CSM parsed: %s (%d state machines)%n", csmClass.name, csmClass.stateMachines.size()));

    // Check state machine
    CollaborativeStateMachineClass csmObject = CollaborativeStateMachineClassBuilder.from(csmClass).build();
    logger.info(String.format("CSM built: %s (%d state machines)%n", csmObject, csmObject.vertexSet().size()));

    var out = new StringWriter();
    try {
      CollaborativeStateMachineExporter.export(out, csmObject);
    } catch (Exception e) {
      logger.severe(String.format("Failed to export collaborative state machine: %s", e.getMessage()));
      System.exit(1);
    }

    var file = new File("plantuml/" + args[0]+ ".puml");
    try (var writer = new FileWriter(file)) {
      writer.write(out.toString());
    }
  }
}
