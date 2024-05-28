# Cirrina Railway Services

This folder contains the implementations of two services: _gate_ and _light_.

To start the services using Uvicorn:

```bash
uvicorn gate:app --port 8001 --workers 4
uvicorn light:app --port 8002 --workers 4
```

Please refer to the Uvicorn documentation for additional configuration parameters.
