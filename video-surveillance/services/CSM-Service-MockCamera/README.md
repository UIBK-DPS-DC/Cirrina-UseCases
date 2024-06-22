# CSM Service Mock Camera

This project provides a mock camera service. The service is specifically designed for Raspberry Pi, and used as a local
IoT service in CSM experiments. The mock camera service is specifically provided as an alternative to the live camera
capture service, for reasons of reproducability. The [BUILDING.md](BUILDING.md) document contains more information about
building this project.

## Running

To run the service, you can use the following command:

```bash
docker compose up --build
```
