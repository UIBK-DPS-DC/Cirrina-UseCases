# Smart Factory Use Case Experiment

- `TestServer` `[<port>]`: Local execution of a HTTP server which simulates endpoints for the smart factory use case 
  service invocations. Runs on `localhost` and takes an optional port (Default `8000`). Can be used in combination 
  with `../csml/service_implementations.json`.
- `TestLocal` `<csmFileName>`: Local execution of a `SharedRuntime`. Takes a CSM file name as a parameter which must be 
  a valid file in `../csml/`.
- `TestPlantUML` `<csmFileName>`: Local creation of PlantUML using a CSM file name as a parameter which must be a valid 
  file in `../csml/`. Stores the resulting `.puml` file as `plantuml/<csmFileName>.puml`.


## Usage

1. Create an environment variable `CIRRINA_HOME` and set its value to the path of the Cirrina root directory (the 
   cloned repository folder).
2. Clone `cirrina-usecases` and build `cirrina-usecases/smart-factory/experiment`:
````bash
git clone https://git.uibk.ac.at/informatik/dps/dps-dc-software/cirrina-usecases.git
cd cirrina-usecases/smart-factory/experiment
./gradlew build
````
3. Run one of the following:
    1. `TestServer` via `runTestServer` goal:
   ````bash
    ./gradlew runTestServer [--args="<port>"]
    ````
    2. `TestLocal` via `runTestLocal` goal:
    ````bash
    ./gradlew runTestLocal --args="smart_factory_<default|develop|extended>.csm"
    ````
   3. `TestPlantUML` via `runTestPlantUML` goal:
    ````bash
    ./gradlew runTestPlantUML --args="smart_factory_<default|develop|extended>.csm"
    ````
    