# Scripts

## create_jobs.py

Takes a CSML file and a service description JSON file.
Creates jobs for all state machines in the CSML file.

### Usage

```
create_jobs.py [-h] --csml_file CSML_FILE --services_file SERVICES_FILE [--host HOST] [--local_data LOCAL_DATA] [--instances INSTANCES]
```

where

```
--host HOST                     ZooKeeper host address
--csml_file CSML_FILE           Path to the CSML file
--services_file SERVICES_FILE   Path to the service descriptions JSON file
--local_data LOCAL_DATA         Local data map in JSON format
--instances INSTANCES           Amount of instances for each state machine
```