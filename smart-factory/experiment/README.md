# Smart Factory Use Case Experiment

- `TestLocal` `<csmFileName>`: Local execution of a `SharedRuntime`. Takes a CSM file name as a parameter which must be 
  a valid file in `../csml/`.
- `TestPlantUML` `<csmFileName>`: Local creation of PlantUML using a CSM file name as a parameter which must be a valid 
  file in `../csml/`. Stores the resulting `.puml` file as `plantuml/<csmFileName>.puml`.


## Usage

1. Create an environment variable `CIRRINA_HOME` and set its value to the path of the Cirrina root directory (the 
   cloned repository folder).
2. Clone and build `cirrina-usecases`:


    git clone https://git.uibk.ac.at/informatik/dps/dps-dc-software/cirrina-usecases.git
    cd smart-factory/experiment
    ./gradlew build
3. Run the `runTestLocal` goal:


    ./gradlew runTestLocal --args="smart_factory_<default|develop|extended>.csm"
    