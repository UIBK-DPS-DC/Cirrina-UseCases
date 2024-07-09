# Smart Factory Use Case HTTP Server

HTTP server with endpoints required by the smart factory use case. Simulates all necessary services. Used by both the 
Cirrina and Sonataflow version of the smart factory use case.

## Usage

### Gradle

```sh
./gradlew runMain [--args="OPTIONS"]
```
where `OPTIONS` may include:

- `--errorStrategy, -e`
  - Error strategy used for scan, pickup, and assemble results.
  - Default: `NO_ERRORS`
  - Possible Values:
    - `NO_ERRORS`: No errors
    - `RANDOM_ERRORS_LOW`: Adds a fixed 20% error rate.
    - `RANDOM_ERRORS_HIGH`: Adds a fixed 80% error rate.
    - `GRADUALLY_INCREASE`: Starts with no errors. Increases the error rate gradually over time.
    - `SUDDEN_PEAK`: Switches between no errors and a 90% error rate after a specific number of invocations.

- `--port, -p`
  - Port number.
  - Default: `8000`

- `--useDelays, -ud`
  - Cause random response delays when a service is invoked.
  - Default: `false`

- `--useProto, -up`
  - Use protobuf for variables (Required by Cirrina, does not work with Sonataflow).
  - Default: `true`

### Docker Compose

```sh
docker-compose up
```
`OPTIONS` can be passed by setting environment variables beforehand: 
- `PROTO` corresponds to `--useProto`, 
- `DELAYS` corresponds to `--useDelays`, and 
- `ERROR_STRATEGY` corresponds to `--errorStrategy`