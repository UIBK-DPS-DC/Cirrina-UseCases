package at.ac.uibk.dps.smartfactory;

import at.ac.uibk.dps.cirrina.core.exception.CirrinaException;
import at.ac.uibk.dps.cirrina.core.io.plantuml.CollaborativeStateMachineExporter;
import at.ac.uibk.dps.cirrina.core.lang.classes.CollaborativeStateMachineClass;
import at.ac.uibk.dps.cirrina.core.lang.parser.Parser;
import at.ac.uibk.dps.cirrina.core.object.collaborativestatemachine.CollaborativeStateMachine;
import at.ac.uibk.dps.cirrina.core.object.collaborativestatemachine.CollaborativeStateMachineBuilder;

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
    String csmDescription = CsmHelper.readCsm(args[0]);
    Parser parser = new Parser(new Parser.Options());

    // Parse state machine
    CollaborativeStateMachineClass csmClass = parser.parse(csmDescription);
    logger.info(String.format("CSM parsed: %s (%d state machines)%n", csmClass.name, csmClass.stateMachines.size()));

    // Check state machine
    CollaborativeStateMachine csmObject = CollaborativeStateMachineBuilder.from(csmClass).build();
    logger.info(String.format("CSM built: %s (%d state machines)%n", csmObject, csmObject.vertexSet().size()));

    var out = new StringWriter();
    try {
      CollaborativeStateMachineExporter.export(out, csmObject);
    } catch (CirrinaException e) {
      logger.severe(String.format("Failed to export collaborative state machine: %s", e.getMessage()));
      System.exit(1);
    }

    var file = new File("plantuml/" + args[0]+ ".puml");
    try (var writer = new FileWriter(file)) {
      writer.write(out.toString());
    }
  }
}
