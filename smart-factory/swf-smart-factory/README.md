# Smart Factory Serverless Workflow

## Requirements

- Java 17+
- Maven 3.8.6+ 
- (Optional) Quarkus CLI
---
- Services: A running instance of `simulation-server` listening on port 8000.<br>
URLs are currently hardcoded in the `<workflow>.sw.json` files.


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

## Interact with the Quarkus application

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


## Problems and Limitations

**Some of the following problems might only apply to the `999-SNAPSHOT` version of Sonataflow (June 2024).**

- The business key functionality requires the latest `999-SNAPSHOT` version to function correctly in combination with CloudEvents (`ce-kogitobusinesskey` has no effect in previous versions). Additionally, Quarkus 3+ is only supported in the latest snapshot versions. Stable versions require Quarkus 2.
<br>**SOLUTION**: We use version `999-SNAPSHOT` instead of a stable version.
- The Sonataflow Dev UI extension requires dependencies which do not support the `999-SNAPSHOT` version of Sonataflow and can thus currently not be used. 
<br>**SOLUTION**: Without the Dev UI, starting workflows or retrieving workflow information can be done by invoking the respective POST or GET requests manually.
- Business keys are not passed to sub-workflows (`subFlow`). This means that sub-workflows can hardly be used on an event-based basis.
- After invoking HTTP requests through functions of type `rest`, all values in the JSON responses are always `null`. This causes functions of type `rest` to be unusable if the service output is necessary. Additionally, the application might randomly throw a fatal exception during startup or when starting a workflow if a function of type `rest` is used.
<br>**SOLUTION**: You may use type `custom` and operation `rest:<method>:<url>` instead, see the workflow json files for examples.
- When invoking HTTP requests through functions of type `custom` and operation `rest:post:<url>`, the entries `Port: null` and `Host: null` are always added to the JSON payload.
- During startup Quarkus logs warnings of the form `Unrecognized configuration key ...`. These configuration keys are in fact recognized and application startup might fail if they are not provided.
- In case a workflow throws an unhandled exception, the entire application enters an error state and it is often required to restart the quarkus application.
- Too many produced CloudEvents may cause the error `SRMSG00034: Insufficient downstream requests to emit item` and consecutively crash the runtime. This will already happen when the runtime needs to handle around 10 events per second (e.g. setting the event rate of both photoelectric sensors to 200ms or lower).