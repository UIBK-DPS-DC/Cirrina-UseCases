# Cirrina Use Cases

Repository containing [Cirrina](https://git.uibk.ac.at/informatik/dps/dps-dc-software/cirrina-project/cirrina) use
cases. Cirrina, a distributed Collaborative State Machines (CSM) runtime for the Cloud-Edge-IoT continuum.

## Current Use Cases

This repository contains the following use case implementations:

- [Railway](railway)<br>
  Simulation of a railway crossing including gate and light management. Serves as a Cirrina stress test.
- [Video Surveillance](video-surveillance)<br>
  Detection of threats through video surveillance. Simulates a Camera (IoT), performs person detection (Edge) to filter
  out unnecessary images, and face analysis (Cloud) to check for unknown persons.
- [Smart Factory](smart-factory)<br>
  Simulates an automated production line by leveraging the full Cloud-Edge-IoT continuum. Includes conveyor belts, robotic
  arms, and camera systems within the Edge-IoT domain, and monitoring and assembly management in the Cloud domain.

Individual use case descriptions and usage instructions can be found in the corresponding README files.

## License

GPLv3 licensed, see [LICENSE](LICENSE).
