import argparse
import configparser
import json
import os
import sys

from typing import Any, Dict, List

MACHINES_JSON = "machines.json"

LOCAL_DATA = {
    "jobControlSystem": {
        "totalProducts": 300
    },
    "roboticArmSystem": {
        "partsPerProduct": 10
    }
}

class Site:
    def __init__(self, proto: bool):
        self.nats: str = ""
        self.zookeeper: str = ""
        self.remote_services: str = ""
        self.local_services: List[str] = []
        self.runtimes: List[str] = []
        self.proto = proto

    def get_nats_host_string(self) -> str:
        return f"ansible_host={self.nats}"

    def get_zookeeper_host_string(self) -> str:
        return f"ansible_host={self.zookeeper}"

    def get_remote_services_host_string(self) -> str:
        return f"ansible_host={self.remote_services} PROTO={'true' if self.proto else 'false'}"

    def get_local_services_host_strings(self) -> List[str]:
        return [
            f"ansible_host={local_service} PROTO={'true' if self.proto else 'false'}"
            for local_service in self.local_services
        ]

    def get_runtime_host_strings(self, global_host: str) -> List[str]:
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

    for i, site in enumerate(sites):
        config.set(
            "services_servers",
            f"remoteservices{i} {site.get_remote_services_host_string()}",
        )

    i = 0
    for site in sites:
        for host_string in site.get_local_services_host_strings():
            config.set(
                "services_servers",
                f"localservices{i} {host_string}",
            )

            i += 1

    # Runtimes
    if cirrina:
        config.add_section("runtime_servers")
    else:
        config.add_section("sonataflow")

    i = 0
    for site in sites:
        for host, host_string in zip(
            site.runtimes, site.get_runtime_host_strings(global_host)
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
                    "sonataflow",
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

    experiment_folder = "experiment" if cirrina else "experiment_sonataflow"

    print(f"Writing inventory to: {experiment_folder}")

    # Write configuration
    with open(f"{experiment_folder}/ansible/inventory/hosts", "w") as configfile:
        config.write(configfile)

    return runtimes


def write_jobs(sites: List[Site], runtimes: Dict[str, str], is_local: bool):
    global_job_description = {}

    global_job_description["localData"] = {}
    global_job_description["bindEventInstanceIds"] = []

    i = 0

    path = "csml/smart_factory.local.csml" if is_local else "csml/smart_factory.remote.csml"

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
            for sm_name in sm_names:
                job_description = global_job_description.copy()

                job_description["serviceImplementations"] = []
                job_description["stateMachineName"] = sm_name

                if sm_name in LOCAL_DATA:
                    job_description["localData"] = LOCAL_DATA[sm_name]

                if is_local:
                    job_description["serviceImplementations"].extend(
                        [
                            {
                                "type": "HTTP",
                                "scheme": "http",
                                "host": host,
                                "port": 8000,
                                "endPoint": "/stopBelt",
                                "method": "POST",
                                "name": "stopBelt",
                                "cost": 1.0,
                                "local": True
                            },
                            {
                                "type": "HTTP",
                                "scheme": "http",
                                "host": host,
                                "port": 8000,
                                "endPoint": "/moveBelt",
                                "method": "POST",
                                "name": "moveBelt",
                                "cost": 1.0,
                                "local": True
                            },
                            {
                                "type": "HTTP",
                                "scheme": "http",
                                "host": host,
                                "port": 8000,
                                "endPoint": "/sendSms",
                                "method": "POST",
                                "name": "sendSms",
                                "cost": 1.0,
                                "local": True
                            },
                            {
                                "type": "HTTP",
                                "scheme": "http",
                                "host": host,
                                "port": 8000,
                                "endPoint": "/beamDetectionStart",
                                "method": "POST",
                                "name": "beamDetectionStart",
                                "cost": 1.0,
                                "local": True
                            },
                            {
                                "type": "HTTP",
                                "scheme": "http",
                                "host": host,
                                "port": 8000,
                                "endPoint": "/sendStatistics",
                                "method": "POST",
                                "name": "sendStatistics",
                                "cost": 1.0,
                                "local": True
                            },
                            {
                                "type": "HTTP",
                                "scheme": "http",
                                "host": host,
                                "port": 8000,
                                "endPoint": "/sendMail",
                                "method": "POST",
                                "name": "sendMail",
                                "cost": 1.0,
                                "local": True
                            },
                            {
                                "type": "HTTP",
                                "scheme": "http",
                                "host": host,
                                "port": 8000,
                                "endPoint": "/scanPhoto",
                                "method": "POST",
                                "name": "scanPhoto",
                                "cost": 1.0,
                                "local": True
                            },
                            {
                                "type": "HTTP",
                                "scheme": "http",
                                "host": host,
                                "port": 8000,
                                "endPoint": "/pickUp",
                                "method": "POST",
                                "name": "pickUp",
                                "cost": 1.0,
                                "local": True
                            },
                            {
                                "type": "HTTP",
                                "scheme": "http",
                                "host": host,
                                "port": 8000,
                                "endPoint": "/beamDetectionEnd",
                                "method": "POST",
                                "name": "beamDetectionEnd",
                                "cost": 1.0,
                                "local": True
                            },
                            {
                                "type": "HTTP",
                                "scheme": "http",
                                "host": host,
                                "port": 8000,
                                "endPoint": "/takePhoto",
                                "method": "POST",
                                "name": "takePhoto",
                                "cost": 1.0,
                                "local": True
                            },
                            {
                                "type": "HTTP",
                                "scheme": "http",
                                "host": host,
                                "port": 8000,
                                "endPoint": "/assemble",
                                "method": "POST",
                                "name": "assemble",
                                "cost": 1.0,
                                "local": True
                            },
                            {
                                "type": "HTTP",
                                "scheme": "http",
                                "host": host,
                                "port": 8000,
                                "endPoint": "/returnToStart",
                                "method": "POST",
                                "name": "returnToStart",
                                "cost": 1.0,
                                "local": True
                            }  
                        ]
                    )
                else:
                    job_description["serviceImplementations"].extend(
                        [
                            {
                                "type": "HTTP",
                                "scheme": "http",
                                "host": site.remote_services,
                                "port": 8000,
                                "endPoint": "/stopBelt",
                                "method": "POST",
                                "name": "stopBelt",
                                "cost": 1.0,
                                "local": True
                            },
                            {
                                "type": "HTTP",
                                "scheme": "http",
                                "host": site.remote_services,
                                "port": 8000,
                                "endPoint": "/moveBelt",
                                "method": "POST",
                                "name": "moveBelt",
                                "cost": 1.0,
                                "local": True
                            },
                            {
                                "type": "HTTP",
                                "scheme": "http",
                                "host": site.remote_services,
                                "port": 8000,
                                "endPoint": "/sendSms",
                                "method": "POST",
                                "name": "sendSms",
                                "cost": 1.0,
                                "local": True
                            },
                            {
                                "type": "HTTP",
                                "scheme": "http",
                                "host": site.remote_services,
                                "port": 8000,
                                "endPoint": "/beamDetectionStart",
                                "method": "POST",
                                "name": "beamDetectionStart",
                                "cost": 1.0,
                                "local": True
                            },
                            {
                                "type": "HTTP",
                                "scheme": "http",
                                "host": site.remote_services,
                                "port": 8000,
                                "endPoint": "/sendStatistics",
                                "method": "POST",
                                "name": "sendStatistics",
                                "cost": 1.0,
                                "local": True
                            },
                            {
                                "type": "HTTP",
                                "scheme": "http",
                                "host": site.remote_services,
                                "port": 8000,
                                "endPoint": "/sendMail",
                                "method": "POST",
                                "name": "sendMail",
                                "cost": 1.0,
                                "local": True
                            },
                            {
                                "type": "HTTP",
                                "scheme": "http",
                                "host": site.remote_services,
                                "port": 8000,
                                "endPoint": "/scanPhoto",
                                "method": "POST",
                                "name": "scanPhoto",
                                "cost": 1.0,
                                "local": True
                            },
                            {
                                "type": "HTTP",
                                "scheme": "http",
                                "host": site.remote_services,
                                "port": 8000,
                                "endPoint": "/pickUp",
                                "method": "POST",
                                "name": "pickUp",
                                "cost": 1.0,
                                "local": True
                            },
                            {
                                "type": "HTTP",
                                "scheme": "http",
                                "host": site.remote_services,
                                "port": 8000,
                                "endPoint": "/beamDetectionEnd",
                                "method": "POST",
                                "name": "beamDetectionEnd",
                                "cost": 1.0,
                                "local": True
                            },
                            {
                                "type": "HTTP",
                                "scheme": "http",
                                "host": site.remote_services,
                                "port": 8000,
                                "endPoint": "/takePhoto",
                                "method": "POST",
                                "name": "takePhoto",
                                "cost": 1.0,
                                "local": True
                            },
                            {
                                "type": "HTTP",
                                "scheme": "http",
                                "host": site.remote_services,
                                "port": 8000,
                                "endPoint": "/assemble",
                                "method": "POST",
                                "name": "assemble",
                                "cost": 1.0,
                                "local": True
                            },
                            {
                                "type": "HTTP",
                                "scheme": "http",
                                "host": site.remote_services,
                                "port": 8000,
                                "endPoint": "/returnToStart",
                                "method": "POST",
                                "name": "returnToStart",
                                "cost": 1.0,
                                "local": True
                            }  
                        ]
                    )

                job_description["runtimeName"] = runtimes[host]

                with open(f"job/job{i}.json", "w") as file:
                    json.dump(job_description, file, indent=4)

                i += 1


if __name__ == "__main__":
    parser = argparse.ArgumentParser()

    parser.add_argument("--local", action=argparse.BooleanOptionalAction, required=True)
    parser.add_argument("--cirrina", action=argparse.BooleanOptionalAction, required=True)

    args = parser.parse_args()

    machines = read_machines_json()
    print(f"no. sites={len(machines)}")

    global_host = machines["global"]
    print(f"global host={global_host}")

    sites = [create_site(nodes, args.cirrina) for _, nodes in machines["sites"].items()]

    runtimes = write_hosts_config(sites, args.cirrina)

    if args.cirrina:
        write_jobs(sites, runtimes, args.local)

    sys.exit(0)
