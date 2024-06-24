# Smart Factory Use Case HTTP Server

HTTP server which simulates endpoints for the smart factory use case service invocations.

## Usage

```sh
./gradlew runMain [--args="OPTIONS"]
```
where OPTIONS:
```
--errorStrategy, -e
  Error strategy used for scan, pickup and assemble results
  Default: NO_ERRORS
  Possible Values: [NO_ERRORS, RANDOM_ERRORS_LOW, RANDOM_ERRORS_HIGH, GRADUALLY_INCREASE, SUDDEN_PEAK]
--port, -p
  Port number
  Default: 0
--useDelays, -ud
  Cause response delays
  Default: false
--useProto, -up
  Use protobuf for variables
  Default: true
```

## Docker Compose

```sh
docker-compose up
```
OPTIONS can be passed by setting environment variables beforehand: `PROTO`, `DELAYS`, `ERROR_STRATEGY` 