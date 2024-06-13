import configparser
import json
import os
import sys

from typing import Any, Dict, List

MACHINES_JSON = "machines.json"


class Site:
    def __init__(self):
        self.nats: str = ""
        self.zookeeper: str = ""
        self.remote_services: str = ""
        self.local_services: List[str] = []
        self.runtimes: List[str] = []

    def get_nats_host_string(self) -> str:
        return f"ansible_host={self.nats}"

    def get_zookeeper_host_string(self) -> str:
        return f"ansible_host={self.zookeeper}"

    def get_remote_services_host_string(self, oltp_host: str) -> str:
        return f"ansible_host={self.remote_services} OTLP_ENDPOINT=http://{oltp_host}:4317/"

    def get_local_services_host_strings(self, global_host: str) -> List[str]:
        return [
            f"ansible_host={local_service} OTLP_ENDPOINT=http://{global_host}:4317/"
            for local_service in self.local_services
        ]

    def get_runtime_host_strings(self, global_host: str) -> List[str]:
        return [
            f"ansible_host={runtime} NATS_PERSISTENT_CONTEXT_URL=nats://{self.nats}:4222/ NATS_EVENT_HANDLER_URL=nats://{self.nats}:4222/ ZOOKEEPER_CONNECT_STRING={self.zookeeper}:2181 OTLP_ENDPOINT=http://{global_host}:4317/"
            for runtime in self.runtimes
        ]


def read_machines_json() -> Any:
    if not os.path.exists(MACHINES_JSON):
        print(f"'{MACHINES_JSON}' does not exist")
        sys.exit(-1)

    with open(MACHINES_JSON) as file:
        machines = json.load(file)

    return machines


def create_site(nodes: List[str]) -> Site:
    site = Site()

    # One per site
    site.nats = nodes[0]
    site.zookeeper = nodes[0]
    site.remote_services = nodes[0]

    # Two per site
    site.local_services.append(nodes[1])
    site.runtimes.append(nodes[1])

    site.local_services.append(nodes[2])
    site.runtimes.append(nodes[2])

    return site


def write_hosts_config(sites: List[Site]):
    runtimes = {}

    class CaseSensitiveConfigParser(configparser.ConfigParser):
        def optionxform(self, optionstr):
            return optionstr

    config = CaseSensitiveConfigParser(allow_no_value=True)

    # NATS
    config.add_section("nats_servers")

    for i, site in enumerate(sites):
        config.set("nats_servers", f"nats{i} {site.get_nats_host_string()}")

    # ZooKeeper
    config.add_section("zookeeper_servers")

    for i, site in enumerate(sites):
        config.set(
            "zookeeper_servers", f"zookeeper{i} {site.get_zookeeper_host_string()}"
        )

    # Services
    config.add_section("services_servers")

    for i, site in enumerate(sites):
        config.set(
            "services_servers",
            f"remoteservices{i} {site.get_remote_services_host_string(global_host)}",
        )

    i = 0
    for site in sites:
        for host_string in site.get_local_services_host_strings(global_host):
            config.set(
                "services_servers",
                f"localservices{i} {host_string}",
            )

            i += 1

    # Runtimes
    config.add_section("runtime_servers")

    i = 0
    for site in sites:
        for host, host_string in zip(
            site.runtimes, site.get_runtime_host_strings(global_host)
        ):
            runtime_name = f"runtime{i}"

            config.set(
                "runtime_servers",
                f"{runtime_name} {host_string} RUNTIME_NAME={runtime_name}",
            )

            runtimes[host] = runtime_name

            i += 1

    # InfluxDB
    config.add_section("influxdb_servers")

    config.set("influxdb_servers", f"influxdb0 ansible_host={global_host}")

    # Telegraf
    config.add_section("telegraf_servers")

    config.set("telegraf_servers", f"telegraf0 ansible_host={global_host}")

    config.add_section("telegraf_servers:vars")

    config.set("telegraf_servers:vars", f"influxdb_url=http://{global_host}:8086/")

    # Simulation
    config.add_section("simulation_servers")

    config.set(
        "simulation_servers",
        f"simulation0 ansible_host={global_host} NATS_URL=nats://{sites[-1].nats}:4222/ OTLP_ENDPOINT=http://{global_host}:4317/",
    )

    # Write configuration
    with open("experiment/ansible/inventory/hosts", "w") as configfile:
        config.write(configfile)

    return runtimes


def write_jobs(sites: List[Site], runtimes: Dict[str, str]):
    global_job_description = {}

    with open("csml/railway.csml") as file:
        global_job_description["collaborativeStateMachine"] = json.load(file)

    global_job_description["stateMachineName"] = "crossing"
    global_job_description["localData"] = {}
    global_job_description["bindEventInstanceIds"] = []

    i = 0
    for site in sites:
        for host in site.runtimes:
            job_description = global_job_description.copy()

            job_description["serviceImplementations"] = [
                {
                    "name": "gateUp",
                    "type": "HTTP",
                    "cost": 0.0,
                    "local": True,
                    "scheme": "http",
                    "host": host,
                    "port": 8001,
                    "endPoint": "/gate/up",
                    "method": "POST",
                },
                {
                    "name": "gateDown",
                    "type": "HTTP",
                    "cost": 0.0,
                    "local": True,
                    "scheme": "http",
                    "host": host,
                    "port": 8001,
                    "endPoint": "/gate/down",
                    "method": "POST",
                },
                {
                    "name": "lightOn",
                    "type": "HTTP",
                    "cost": 0.0,
                    "local": True,
                    "scheme": "http",
                    "host": host,
                    "port": 8002,
                    "endPoint": "/light/on",
                    "method": "POST",
                },
                {
                    "name": "lightOff",
                    "type": "HTTP",
                    "cost": 0.0,
                    "local": True,
                    "scheme": "http",
                    "host": host,
                    "port": 8002,
                    "endPoint": "/light/off",
                    "method": "POST",
                },
                {
                    "name": "gateUp",
                    "type": "HTTP",
                    "cost": 0.0,
                    "local": False,
                    "scheme": "http",
                    "host": site.remote_services,
                    "port": 8001,
                    "endPoint": "/gate/up",
                    "method": "POST",
                },
                {
                    "name": "gateDown",
                    "type": "HTTP",
                    "cost": 0.0,
                    "local": False,
                    "scheme": "http",
                    "host": site.remote_services,
                    "port": 8001,
                    "endPoint": "/gate/down",
                    "method": "POST",
                },
                {
                    "name": "lightOn",
                    "type": "HTTP",
                    "cost": 0.0,
                    "local": False,
                    "scheme": "http",
                    "host": site.remote_services,
                    "port": 8002,
                    "endPoint": "/light/on",
                    "method": "POST",
                },
                {
                    "name": "lightOff",
                    "type": "HTTP",
                    "cost": 0.0,
                    "local": False,
                    "scheme": "http",
                    "host": site.remote_services,
                    "port": 8002,
                    "endPoint": "/light/off",
                    "method": "POST",
                },
            ]

            job_description["runtimeName"] = runtimes[host]

            with open(f"job/job{i}.json", "w") as file:
                json.dump(job_description, file, indent=4)

            i += 1


if __name__ == "__main__":
    machines = read_machines_json()
    print(f"no. sites={len(machines)}")

    global_host = machines["global"]
    print(f"global host={global_host}")

    sites = [create_site(nodes) for _, nodes in machines["sites"].items()]

    runtimes = write_hosts_config(sites)

    write_jobs(sites, runtimes)

    sys.exit(0)
