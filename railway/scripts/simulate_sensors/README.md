# Cirrina Railway Sensor Simulation

This folder contains the implementations of the sensor simulation.

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
opentelemetry-instrument --metric_export_interval 1000 --metrics_exporter otlp python simulate_sensors.py nats://localhost:4222
```

Where the export interval can be configured as desired.
