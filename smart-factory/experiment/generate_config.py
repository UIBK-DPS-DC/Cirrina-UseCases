import argparse
import configparser
import json
import os
import sys

from typing import Any, Dict, List

MACHINES_JSON = "machines.json"

LOCAL_DATA = {
    "jobControlSystem": {
        "totalProducts": 0
    },
    "roboticArmSystem": {
        "partsPerProduct": 10
    }
}

class Site:
    def __init__(self, proto: bool):
        self.nats: str = ""
        self.zookeeper: str = ""
        self.runtimes: List[str] = []
        self.proto = proto

    def get_nats_host_string(self) -> str:
        return f"ansible_host={self.nats}"

    def get_zookeeper_host_string(self) -> str:
        return f"ansible_host={self.zookeeper}"

    def get_runtime_host_strings(self) -> List[str]:
        env = f" NATS_PERSISTENT_CONTEXT_URL=nats://{self.nats}:4222/ NATS_EVENT_HANDLER_URL=nats://{self.nats}:4222/ ZOOKEEPER_CONNECT_STRING={self.zookeeper}:2181 OTLP_ENDPOINT=http://{global_host}:4317/" \
            if self.proto else ""

        return [
            f"ansible_host={runtime}{env}"
            for runtime in self.runtimes
        ]


def read_machines_json() -> Any:
    if not os.path.exists(MACHINES_JSON):
        print(f"'{MACHINES_JSON}' does not exist")
        sys.exit(-1)

    with open(MACHINES_JSON) as file:
        machines = json.load(file)

    return machines


def create_site(nodes: List[str], proto: bool) -> Site:
    site = Site(proto)

    # Remote
    site.nats = nodes[0]
    site.zookeeper = nodes[0]

    # Runtimes
    site.runtimes.append(nodes[1])
    site.runtimes.append(nodes[2])

    return site


def write_hosts_config(sites: List[Site], cirrina: bool):
    runtimes = {}

    class CaseSensitiveConfigParser(configparser.ConfigParser):
        def optionxform(self, optionstr):
            return optionstr

    config = CaseSensitiveConfigParser(allow_no_value=True)

    if cirrina:
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

    config.set(
        "services_servers",
        f"remoteservices0 ansible_host={global_host} PROTO={'true' if cirrina else 'false'}",
    )

    # Runtimes
    if cirrina:
        config.add_section("runtime_servers")
    else:
        config.add_section("sonataflow_servers")

    i = 0
    for site in sites:
        for host, host_string in zip(
            site.runtimes, site.get_runtime_host_strings()
        ):
            runtime_name = f"runtime{i}"

            if cirrina:
                config.set(
                    "runtime_servers",
                    f"{runtime_name} {host_string} RUNTIME_NAME={runtime_name}",
                )

                runtimes[host] = runtime_name
            else:
                config.set(
                    "sonataflow_servers",
                    f"{runtime_name} {host_string}"
                )

            i += 1
    

    # InfluxDB
    config.add_section("global_servers")

    env = f" INFLUXDB_URL=http://{global_host}:8086/ NATS_URL=nats://{sites[-1].nats}:4222/ OTLP_ENDPOINT=http://{global_host}:4317/" \
        if cirrina else ""
    config.set(
        "global_servers",
        f"global0 ansible_host={global_host}{env}",
    )

    experiment_folder = "cirrina" if cirrina else "sonataflow"

    print(f"Writing inventory to: {experiment_folder}")

    # Write configuration
    with open(f"{experiment_folder}/ansible/inventory/hosts", "w") as configfile:
        config.write(configfile)

    return runtimes


def write_jobs(sites: List[Site], runtimes: Dict[str, str]):
    global_job_description = {}

    global_job_description["localData"] = {}
    global_job_description["bindEventInstanceIds"] = []

    i = 0

    path = "../csml/smart_factory_experiment.csml"

    with open(path) as file:
        global_job_description["collaborativeStateMachine"] = json.load(file)

    sm_names = list(
        map(
            lambda sm: sm["name"],
            filter(
                lambda sm: "abstract" not in sm or not sm["abstract"],
                global_job_description["collaborativeStateMachine"]["stateMachines"],
            ),
        )
    )
    
    for site in sites:
        for host_index, host in enumerate(site.runtimes):
            for j in range(5):
                for sm_name in sm_names:
                    if sm_name == "jobControlSystem" and j != 0:
                        continue

                    job_description = global_job_description.copy()

                    job_description["serviceImplementations"] = []
                    job_description["stateMachineName"] = sm_name

                    if sm_name in LOCAL_DATA:
                        job_description["localData"] = LOCAL_DATA[sm_name]
                    
                    job_description["serviceImplementations"].extend(
                        [
                            {
                                "type": "HTTP",
                                "scheme": "http",
                                "host": global_host,
                                "port": 8000,
                                "endPoint": "/stopBelt",
                                "method": "POST",
                                "name": "stopBelt",
                                "cost": 1.0,
                                "local": False
                            },
                            {
                                "type": "HTTP",
                                "scheme": "http",
                                "host": global_host,
                                "port": 8000,
                                "endPoint": "/moveBelt",
                                "method": "POST",
                                "name": "moveBelt",
                                "cost": 1.0,
                                "local": False
                            },
                            {
                                "type": "HTTP",
                                "scheme": "http",
                                "host": global_host,
                                "port": 8000,
                                "endPoint": "/sendSms",
                                "method": "POST",
                                "name": "sendSms",
                                "cost": 1.0,
                                "local": False
                            },
                            {
                                "type": "HTTP",
                                "scheme": "http",
                                "host": global_host,
                                "port": 8000,
                                "endPoint": "/beamDetectionStart",
                                "method": "POST",
                                "name": "beamDetectionStart",
                                "cost": 1.0,
                                "local": False
                            },
                            {
                                "type": "HTTP",
                                "scheme": "http",
                                "host": global_host,
                                "port": 8000,
                                "endPoint": "/sendStatistics",
                                "method": "POST",
                                "name": "sendStatistics",
                                "cost": 1.0,
                                "local": False
                            },
                            {
                                "type": "HTTP",
                                "scheme": "http",
                                "host": global_host,
                                "port": 8000,
                                "endPoint": "/sendMail",
                                "method": "POST",
                                "name": "sendMail",
                                "cost": 1.0,
                                "local": False
                            },
                            {
                                "type": "HTTP",
                                "scheme": "http",
                                "host": global_host,
                                "port": 8000,
                                "endPoint": "/scanPhoto",
                                "method": "POST",
                                "name": "scanPhoto",
                                "cost": 1.0,
                                "local": False
                            },
                            {
                                "type": "HTTP",
                                "scheme": "http",
                                "host": global_host,
                                "port": 8000,
                                "endPoint": "/pickUp",
                                "method": "POST",
                                "name": "pickUp",
                                "cost": 1.0,
                                "local": False
                            },
                            {
                                "type": "HTTP",
                                "scheme": "http",
                                "host": global_host,
                                "port": 8000,
                                "endPoint": "/beamDetectionEnd",
                                "method": "POST",
                                "name": "beamDetectionEnd",
                                "cost": 1.0,
                                "local": False
                            },
                            {
                                "type": "HTTP",
                                "scheme": "http",
                                "host": global_host,
                                "port": 8000,
                                "endPoint": "/takePhoto",
                                "method": "POST",
                                "name": "takePhoto",
                                "cost": 1.0,
                                "local": False
                            },
                            {
                                "type": "HTTP",
                                "scheme": "http",
                                "host": global_host,
                                "port": 8000,
                                "endPoint": "/assemble",
                                "method": "POST",
                                "name": "assemble",
                                "cost": 1.0,
                                "local": False
                            },
                            {
                                "type": "HTTP",
                                "scheme": "http",
                                "host": global_host,
                                "port": 8000,
                                "endPoint": "/returnToStart",
                                "method": "POST",
                                "name": "returnToStart",
                                "cost": 1.0,
                                "local": False
                            }  
                        ]
                    )

                    job_description["runtimeName"] = runtimes[host]

                    with open(f"cirrina/job/job{i}.json", "w") as file:
                        json.dump(job_description, file, indent=4)

                    i += 1


if __name__ == "__main__":
    parser = argparse.ArgumentParser()

    parser.add_argument("--cirrina", action=argparse.BooleanOptionalAction, required=True)
    parser.add_argument("--products", type=int, default=100, required=False)

    args = parser.parse_args()

    LOCAL_DATA["jobControlSystem"]["totalProducts"] = args.products

    machines = read_machines_json()
    print(f"no. sites={len(machines)}")

    global_host = machines["global"]
    print(f"global host={global_host}")

    sites = [create_site(nodes, args.cirrina) for _, nodes in machines["sites"].items()]

    runtimes = write_hosts_config(sites, args.cirrina)

    if args.cirrina:
        write_jobs(sites, runtimes)

    sys.exit(0)
