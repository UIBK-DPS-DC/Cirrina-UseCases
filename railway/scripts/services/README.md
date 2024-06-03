# Cirrina Railway Services

This folder contains the implementations of two services: _gate_ and _light_.

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
