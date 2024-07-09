# Smart Factory Serverless Workflow

## Contents

- Usage
- Problems and limitations
- Feature comparison SWF/Sonataflow and Cirrina
- Deployment

## Usage

## Requirements

- Java 17+
- Maven 3.8.6+
- (Optional) Quarkus CLI
- (Optional) Serverless Workflow VSCode Extension (by serverlessworkflow)
    - Syntax highlighting and error checking for workflow JSON/YAML files.
- (Optional) REST Client VSCode extension (by Huachao Mao)
    - Easily invoke HTTP requests from the VSCode editor (See [`probe.http`](src/test/resources/probe.http))

---
- Services: 
    - A running instance of [`CSM-Service-MockCamera`](../services/CSM-Service-MockCamera/README.md) listening on port 8001.
    - A running instance of [`CSM-Service-ObjectDetection`](../services/CSM-Service-ObjectDetection/README.md) listening on port 8000.
    - Services must be run with the environment variable `PROTO=false`<br>i.e.
    ```sh
    PROTO=false docker-compose up
    ```

URLs and ports are currently hardcoded in the `<workflow>.sw.json` files and must be adjusted there.


## Start the Quarkus application in dev mode

```sh
mvn clean quarkus:dev
```

or

```sh
quarkus dev
```

- The Quarkus application listens on port 8080
- The Developer UI is accessible at http://localhost:8080/q/dev

## Build and run 

Build and run jar locally:

```sh
mvn clean package
java -jar target/quarkus-app/quarkus-run.jar
```

## Docker deployment

Build docker image:

```sh
mvn clean package
docker build -f src/main/docker/Dockerfile.jvm -t <name> .
```

Run the container:

```sh
docker run -i --rm -p 8080:8080 <name>
```

## Docker Compose deployment

Run with Docker Compose:

```sh
docker-compose up
```

## Script to start workflows

To run all workflows and effectively execute the use case the python script [`start_workflows.py`](scripts/start_workflows.py) can be executed:

```sh
python scripts/start_workflows.py
```

Inputs and URLs are currently hardcoded in the script (TODO create a more general script?). Ensure that both the quarkus application and the simulation server are running before executing the script.

## Manually interact with the Quarkus application

To manually interact with the quarkus application you can make use of the following HTTP requests.

### Execute workflows 

```http
POST http://localhost:8080/<workflow_id>?businessKey=<business_key>
Content-Type: application/json
```

where:
- `<workflow_id>` corresponds to the `id` property defined in the `<workflow>.sw.json` file of the workflow to be executed.
- `<business_key>` can be any string. Workflows launched with a specific business key can consume CloudEvents produced by a workflow with the same business key. If no business key is provided, produced CloudEvents must contain the header `ce-kogitoprocrefid` which is the instance id (UUID) of the workflow which should consume the CloudEvent.

**The business key functionality currently requires the latest snapshot version `999-SNAPSHOT` of Sonataflow (June 2024).**

### Get running workflows (instance ids)

By workflow id:
```http
GET http://localhost:8080/<workflow_id>
```

By workflow id and business key:
```http
GET http://localhost:8080/<workflow_id>?businessKey=<business_key>
```

### Query data index

**REQUIRES `quarkus.kogito.devservices.enabled=true`**

The data index stores information about all workflows (start, end, states entered/left, current state, etc.) and can be useful to collect more information about a running workflow. The data index can be queried using GraphQL manually or by using the UI at http://localhost:8180/graphql.

By default, the data index is in-memory but this can be changed through extensions.

```http
GET http://localhost:8180/<workflow_instance_id>/tasks

{
  "query": "{ 
    ProcessInstances(where: { id: { equal: \"{<workflow_instance_id>}\" } }) {
        id
        processId
        processName
        start
        end
        state
    }
  }"
}
```

Adjust the query

### Manually send CloudEvents

```http
POST http://localhost:8080/
Content-Type: application/json
Accept: application/json
ce-specversion: 1.0
ce-kogitoproctype: SW
ce-kogitoprocist: Active
ce-kogitoprocversion: 1.0.0
ce-kogitoprocid: <source_workflow_id>
ce-id: <unique_cloud_event_uuid>
ce-source: <cloud_event_source>
ce-type: <cloud_event_type>
ce-kogitobusinesskey: <target_workflow_business_key>
```

where:
- `<source_workflow_id>` is the instance id of the source workflow. Not checked, can thus be any UUID.
- `<unique_cloud_event_uuid>` is any UUID which was not yet seen by the target workflow.
- `<cloud_event_source>` is the source CloudEvent. Not used in Sonataflow and can thus be any string.
- `<cloud_event_type>` is the type of the CloudEvent. Defines which event should be consumed (e.g. `product-complete`, `job-done`, etc.).
- `<target_workflow_business_key>` is the business key of the workflow which should consume the event.

`ce-kogitobusinesskey` can be replaced with `ce-kogitoprocrefid` (the instance id of the target workflow) if the workflow has no business key.


# Problems and limitations

## Feature limitations

- Produced CloudEvents can only be consumed **once** by a single workflow.
<br>**SOLUTION**: If multiple workflows need to consume an event, sending it multiple times (preferably different events) is the only solution we found as of right now. See state `jobDone` in `job_control.sw.json` for an example.
- States of type `event` can consume multiple event types, but they do not allow conditional transitions based on which event was consumed.
<br>**SOLUTION**: The `actions` construct in an `onEvents` definition can be used to conditionally assign workflow data based on which event was consumed. This can then be combined with an additional `switch` state which performs conditional transitions based on the assigned workflow data.
- The first state of a workflow (state defined in `start`) can not be targeted by any other transitions. It can thus only be executed once per workflow instance. Usually it makes sense to start a workflow with a state of type `inject` which adds some initial variables and is never entered again during the execution of the workflow.

## Sonataflow related

**Some of the following problems might only apply to the `999-SNAPSHOT` version of Sonataflow (June 2024).**

- The business key functionality requires the latest `999-SNAPSHOT` version to function correctly in combination with CloudEvents (`ce-kogitobusinesskey` has no effect in previous versions). Additionally, Quarkus 3+ (Java 17+) is only supported in the latest snapshot versions. Stable versions require Quarkus 2 as well as Java 11.
<br>**SOLUTION**: We use version `999-SNAPSHOT` instead of a stable version.
- The Sonataflow Dev UI extension requires dependencies which do not support the `999-SNAPSHOT` version of Sonataflow and can thus currently not be used. 
<br>**SOLUTION**: Without the Dev UI, starting workflows or retrieving workflow information can be done by invoking the respective POST or GET requests manually.
- Business keys are not propagated to sub-workflows (`subFlow`) when the parent workflow has a business key. This means that sub-workflows can hardly be used on an event-based basis. There is an open GitHub issue regarding this problem so it might be fixed already.
- After invoking HTTP requests through functions of type `rest`, all values in the JSON responses are always `null`. This causes functions of type `rest` to be unusable if the service output is necessary. Additionally, the application might randomly throw a fatal exception (`specs/services.json not found`) during startup or when starting a workflow if a function of type `rest` is used.
<br>**SOLUTION**: You may use type `custom` and operation `rest:<method>:<url>` instead, see the workflow json files for examples.
- When invoking HTTP requests through functions of type `custom` and operation `rest:post:<url>`, the entries `Port: null` and `Host: null` are always added to the JSON payload.
- During startup Quarkus logs warnings of the form `Unrecognized configuration key ...`. These configuration keys are in fact recognized and application startup might fail if they are not provided.
- In case a workflow throws an unhandled exception (e.g. HTTP error during service invocation), the entire application enters an error state and it is often required to restart the quarkus application.
- Too many produced CloudEvents may cause the error `SRMSG00034: Insufficient downstream requests to emit item` and consecutively crash the runtime. This will already happen when the runtime needs to handle around 10 events per second (e.g. by setting the event rate of both photoelectric sensors to 200ms or lower).
- The sonataflow data index docker image might be unavailable on startup, producing the error message `manifest for apache/incubator-kie-kogito-data-index-ephemeral:main not found: manifest unknown: manifest unknown`.
<br>**SOLUTION**: Temporary add/uncomment `quarkus.kogito.devservices.enabled=false` in `application.properties`. Keep in mind that this disables the default in-memory data index (graphql endpoint).

# Feature comparison SWF/Sonataflow and Cirrina

| Category         | Sonataflow / Serverless Workflows                                                                                                                                                                                                                                                                                                                                                                                                | Cirrina                                                                                                                                                                                                                            |
|------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Control Flow** | Workflow (input/output),<br>Event-based with some [limitations](#problems-and-limitations) (HTTP, KNative or Kafka)                                                                                                                                                                                                                                                                                                              | Event-based (Nats)                                                                                                                                                                                                                 |
| **Components**   | Workflows with states, sub-workflows (subflows).                                                                                                                                                                                                                                                                                                                                                                                 | CSM, state machines with states, nested state machines.                                                                                                                                                                            |
| **Data**         | Data is transferred from state to state and can be filtered. Output of a state is used as input to the next state. Single scope (state data). No built in persistent data.                                                                                                                                                                                                                                                       | All components store data independently. Multiple scopes (state/state machine/CSM, local/persistent/static). Built in persistent data via NATS.                                                                                    |
| **Events**       | CloudEvents which have a type (purpose/nature of event) and source (event producer) as well as data. Sonataflow supports events over HTTP, KNative or Kafka.                                                                                                                                                                                                                                                                     | Handle events locally (internal events) or via NATS. Events can have event data and a channel (NATS subject) which allows to define which state machines receive the event.                                                        |
| **Functions**    | Functions which allow to invoke services (type `rest` or `custom`) and manipulate data (type `expression`). Events can be raised before a transition takes place (`transition` property) or when a workflow stops (`end` property).                                                                                                                                                                                              | Actions which allow to manipulate data, raise events and invoke service types.                                                                                                                                                     |
| **State type**   | Multiple state types where each one has its own functionality. Often requires multiple states to be chained together to achieve a specific goal (e.g. event based transitions require both an event state and a switch state).                                                                                                                                                                                                   | Single flexible state type with multiple functionalities. A single state can do many things at once.                                                                                                                               |
|                  | Event states: Trigger transitions based on one or more events. Transition always ends up in one specific state.                                                                                                                                                                                                                                                                                                                  | "on" transitions:  Trigger transitions based on events. Transitions can end up in different states.                                                                                                                                |
|                  | Operation state: Trigger functions (sync or async) when the state is entered, then take a transition.                                                                                                                                                                                                                                                                                                                            | "entry" actions: Trigger actions when the state is entered. "exit" actions allow for executing actions when a state is exited.                                                                                                     |
|                  | Switch state: Trigger one of multiple transitions based on workflow data.                                                                                                                                                                                                                                                                                                                                                        | Allows data-based transitions using guards. Using the "else" keyword allows to end up in one of two targets. Multiple options through a "match" action within the "entry" actions which raise internal events caught by the state. |
|                  | Inject state: Inject static data into the workflow state data.                                                                                                                                                                                                                                                                                                                                                                   | "Assign/Create" actions: Can be used in various ways to inject static data.                                                                                                                                                        |
|                  | Parallel state: Parallel execution of branches in the workflow (set of states). Additionally sub-workflows can be used for parallelization.                                                                                                                                                                                                                                                                                      | Nested state machines: Parallel execution of state machines.                                                                                                                                                                       |
|                  | For-each state: Executes a set of states in parallel or sequentially for each element of a data array.                                                                                                                                                                                                                                                                                                                           | /                                                                                                                                                                                                                                  |
|                  | Sleep state: After entering this state the workflow instance waits for a specified duration until it moves to the next state.<br>`timeouts` property: Can be defined within all states (except inject) or within the workflow itself. End state execution or workflow execution. State timeouts allow transitions to the next state as soon as the timeout triggers which can be used to e.g. skip an event within event states. | "After" actions: Perform actions after a specified timeout.                                                                                                                                                                        |