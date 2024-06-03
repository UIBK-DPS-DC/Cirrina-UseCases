# Cirrina Railway Experiment

## Introduction

This project, the Cirrina Railway Experiment, aims to demonstrate the utilization and performance of the Cirrina runtime, particularly focusing
on communication between nodes in a Grid'5000 setup. This README provides detailed instructions on setting up the experiment environment,
including the installation of necessary tools like Ansible and the configuration of resources.

## Grid5000 Setup

Before proceeding, ensure familiarity with the [Grid'5000 Getting Started Page](https://www.grid5000.fr/w/Getting_Started) to understand the
environment and execute commands appropriately.

### Step 1: Install Ansible

Ansible is a crucial tool for automating the configuration of nodes in the Grid'5000 environment. To install Ansible, execute the following
commands:

```bash
export PATH=$HOME/.local/bin:$PATH
pip install --user ansible
```

### Step 2: Request Resources

Request the necessary resources on Grid'5000 following your experiment requirements.

### Step 3: Install Environment using Kadeploy

The environment for the experiment, based on Ubuntu 22.04, is installed on the allocated nodes using Kadeploy. Execute the following command:

```bash
kadeploy3 ubuntu2204-min -k
```

The `-k` option ensures that kadeploy copies the SSH public keys from the frontend account to the root account on each allocated node.

### Step 4: Configure Inventory

To configure Ansible properly, create an inventory file reflecting the nodes allocated to your experiment. You can view the available nodes
using the `oarstat` command:

```bash
oarstat -u -f
```

### Step 5: Configure using Ansible

Utilize Ansible to configure the allocated nodes with the necessary software and settings by executing the following playbook:

```bash
ansible-playbook -i inventory/hosts playbook.yml
```

## NATS

NATS is a lightweight and high-performance messaging system. Here's how to interact with it within the experiment environment:

### Benchmarking NATS

Benchmark NATS performance from a host with access to configured NATS servers using the following command:

```bash
nats -s nats://node:4222 bench test --pub 1 --size 16 --msgs 10000000
```

### Retrieving NATS Server Information

Retrieve NATS server/cluster information using the following command:

```bash
nats --user=admin --password=admin server list
```

Ensure to replace the username and password with your configured credentials if different.

### Testing NATS Cluster Functionality

To verify the correct functioning of the NATS cluster, publish messages from one node and subscribe on another within the cluster. Utilize a
tool like `tmux` for easier observation.

On the publishing node:

```bash
nats -s nats://node1:4222 bench test --pub 1 --size 16 --msgs 10000000
```

On the subscribing node:

```bash
nats -s nats://node2:4222 subscribe test
```

Observe incoming events on the subscribing node to confirm proper communication within the NATS cluster.

## Conclusion

Following these steps ensures the successful setup and evaluation of the Cirrina Railway Experiment within the Grid'5000 environment.
Adjustments may be necessary based on specific experiment requirements and configurations.

### Testing ZooKeeper Cluster Functionality

To verify the correct functioning of the ZooKeeper cluster, the monitor command can be sent to a node in the ZooKeeper cluster using:

```bash
echo mntr | nc node 2181
```

Or to get the status of a ZooKeeper node:

```bash
echo stat | nc node 2181
```
