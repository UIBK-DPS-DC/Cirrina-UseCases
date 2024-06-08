#!/usr/bin/python

import re
import sys

from collections import OrderedDict, defaultdict
from dataclasses import dataclass


@dataclass
class Node:
    hostname: str
    name: str


# Parse machines file
with open("machines", "r") as file:
    lines = file.readlines()

lines = sorted(set(line.strip() for line in lines if line.strip()))

site_nodes = {}

for line in lines:
    match = re.search(r"(.*)\.(.*)\.grid5000\.fr", line)

    if not match:
        print(f"invalid format: {line}")
        sys.exit(-1)

    hostname = match.group(0)

    name = match.group(1)
    site = match.group(2)

    if site not in site_nodes:
        site_nodes[site] = []

    site_nodes[site].append(Node(hostname=hostname, name=name))

site_nodes = OrderedDict(sorted(site_nodes.items()))

# Counts per site
site_count = defaultdict(int)
for site, nodes in site_nodes.items():
    for _ in nodes:
        site_count[site] += 1

# Check expected configuration (4 times 3 nodes and 1 time 4 nodes)
hist = {}
for site, nodes in site_count.items():
    if nodes not in [3, 4]:
        print(f"invalid configuration: {nodes}")

    if nodes not in hist:
        hist[nodes] = 0

    hist[nodes] += 1

if hist[3] != 4 or hist[4] != 1:
    print(f"invalid configuration: {hist}")

# Add host lines
nats_servers = []
zookeeper_servers = []
services_servers = []
simulation_servers = []

runtime_servers = []

influxdb_server = []
telegraf_server = []

for site, nodes in site_nodes.items():
    nats_servers.append(f"ansible_host={nodes[0].hostname}")
    zookeeper_servers.append(f"ansible_host={nodes[0].hostname}")
    services_servers.append(f"ansible_host={nodes[0].hostname}")
    simulation_servers.append(f"ansible_host={nodes[0].hostname}")

    runtime_servers.append(
        f"ansible_host={nodes[1].hostname} NATS_PERSISTENT_CONTEXT_URL=nats://{nodes[0].hostname}:4222/ NATS_EVENT_HANDLER_URL=nats://{nodes[0].hostname}:4222/ ZOOKEEPER_CONNECT_STRING={nodes[0].hostname}:2181"
    )
    runtime_servers.append(
        f"ansible_host={nodes[2].hostname} NATS_PERSISTENT_CONTEXT_URL=nats://{nodes[0].hostname}:4222/ NATS_EVENT_HANDLER_URL=nats://{nodes[0].hostname}:4222/ ZOOKEEPER_CONNECT_STRING={nodes[0].hostname}:2181"
    )

    if site_count[site] == 4:
        influxdb_host = nodes[3].hostname
        telegraf_host = nodes[3].hostname

# Output
print("[nats_servers]")
for i, server in enumerate(nats_servers):
    print(f"nats{i} {server}")
print()

print("[zookeeper_servers]")
for i, server in enumerate(nats_servers):
    print(f"zookeeper{i} {server}")
print()

print("[services_servers]")
for i, server in enumerate(services_servers):
    print(f"services{i} {server} OTLP_ENDPOINT=http://{telegraf_host}:4317/")
print()

print("[simulation_servers]")
for i, server in enumerate(simulation_servers):
    print(f"simulation{i} {server} OTLP_ENDPOINT=http://{telegraf_host}:4317/")
print()

print("[runtime_servers]")
for i, server in enumerate(runtime_servers):
    print(
        f"runtime{i} {server} OTLP_ENDPOINT=http://{telegraf_host}:4317/ RUNTIME_NAME=runtime{i}"
    )
print()

print(
    f"""[influxdb_servers]
influxdb0 ansible_host={telegraf_host}"""
)
print()

print(
    f"""[telegraf_servers]
telegraf0 ansible_host={telegraf_host}"""
)
print()

print(
    f"""[telegraf_servers:vars]
influxdb_url=http://{telegraf_host}:8086"""
)

sys.exit(0)
