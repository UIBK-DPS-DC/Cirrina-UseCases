#!/usr/bin/bash

set +e

docker build --pull -t "marlonetheredgeuibk/cirrina-examples-railway-gate:develop" -f services/Gate.Dockerfile services
docker build --pull -t "marlonetheredgeuibk/cirrina-examples-railway-light:develop" -f services/Light.Dockerfile services
docker build --pull -t "marlonetheredgeuibk/cirrina-examples-railway-simulate-sensors:develop" -f simulate_sensors/Dockerfile simulate_sensors

docker push "index.docker.io/marlonetheredgeuibk/cirrina-examples-railway-gate:develop"
docker push "index.docker.io/marlonetheredgeuibk/cirrina-examples-railway-light:develop"
docker push "index.docker.io/marlonetheredgeuibk/cirrina-examples-railway-simulate-sensors:develop"