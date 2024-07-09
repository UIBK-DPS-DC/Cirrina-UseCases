# CSML Files

Contains the video surveillance CSML file `surveillance-system.csml`. Also contains `service_implementations.json` 
which can be used to map service types to the `iotservices`, `edgeservices`, and `cloudservices` endpoints if run 
locally.

## Local usage with Cirrina

- Ensure `iotservices`, `edgeservices`, and `cloudservices` are running with `PROTO=true`. Instructions can be found in
the [corresponding folder](../scripts).

- Launch InfluxDB, Telegraf, NATS and ZooKeeper. This can be done by running the 
[Cirrina compose.yaml](https://git.uibk.ac.at/informatik/dps/dps-dc-software/cirrina-project/cirrina/-/blob/develop/compose.yaml)
using Docker compose:

```
git clone git@git.uibk.ac.at:informatik/dps/dps-dc-software/cirrina-project/cirrina.git
cd cirrina
docker compose up
```

- Create the ZooKeeper nodes (Cirrina jobs). This can be done manually or by using the `create_jobs.py` script which
can be found in the [scripts folder](../../scripts) of this repository. Locally with a single instance, the script can
be run as follows:

```
cd scripts
pip install -r requirements.txt
python create_jobs.py \
    --csml_file "../video-surveillance/csml/surveillance-system.csml" \
    --services_file "../video-surveillance/csml/service_implementations.json"
```

- Run Cirrina, e.g. by either
  - running the [Cirrina Docker Image](https://hub.docker.com/r/marlonetheredgeuibk/cirrina) as explained in the 
  [Cirrina repository](https://git.uibk.ac.at/informatik/dps/dps-dc-software/cirrina-project/cirrina); or by
  - building and running Cirrina manually using the 
  [Cirrina Dockerfile](https://git.uibk.ac.at/informatik/dps/dps-dc-software/cirrina-project/cirrina/-/blob/develop/Dockerfile).