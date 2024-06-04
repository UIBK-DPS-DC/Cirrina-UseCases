# Cirrina Railway Experiment Scripts

## Services

### Gate/Light

These services correspond to the operations of opening and closing a railway crossing gate
and turning the crossing lights on and off, respectively.

To start the services using Uvicorn, run the following commands:

```bash
uvicorn gate:app --port 8001 --workers 4
uvicorn light:app --port 8002 --workers 4
```

Please refer to the Uvicorn documentation for additional configuration parameters.

Note that the port numbers here must match with the service implementations provided in
Cirrina job file(s).

### Sensor Simulation

The sensor simulation simulates the events of three sensors, before, at, and after a railway crossing.
A train with a certain speed and length arrives at a set interval:

```
             Sensor 1         Sensor 2         Sensor 3
               |                |                |
               v                v                v
-----------------------------------------------------
                        _____TRAIN_____
                      //               \\
                     //                 \\
                    //                   \\
                   //                     \\
-----------------------------------------------------
               ^                ^                ^
               |                |                |
```

Sensor values of the three sensors are transmitted through NATS events.

To start the simulation:

```bash
python simulate_sensors.py nats://localhost:4222
```

Where the NATS connection URL is expected to be a valid connection URL.

To instrument the simulation script using OpenTelemetry, and using an OLTP collector, the following simulation
needs to be executed as follows:

```bash
opentelemetry-instrument \
    --metric_export_interval 1000 \
    --metrics_exporter otlp
    python simulate_sensors.py nats://localhost:4222
```

Where the export interval can be configured as desired.

## Running

To run, make use of the provided Docker compose configuration. Bring the services up as follows:

```bash
OTEL_METRIC_EXPORT_INTERVAL=1000 OTEL_METRICS_EXPORTER=oltp NATS_URL=nats://nats-server-host:4222/ docker-compose up --build
```
