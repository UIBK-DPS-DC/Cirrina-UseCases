# Smart Factory Serverless Workflow

## Requirements

- Java 17+
- Maven 3.8.6+ 
- (Optional) Quarkus CLI
---
- Services: A running instance of `simulation-server` listening on port 8000.<br>
Adjust `<service-name>.url=...` in `src/main/resources/application.properties` to change the service URL.


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
