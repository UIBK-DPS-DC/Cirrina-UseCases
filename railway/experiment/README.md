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

Request the necessary resources on Grid'5000 following your experiment requirements. To request a total of 16 nodes spread across 5 sites
(Grenoble, Nantes, Sophia, Rennes, Nancy), we utilize [Funk]():

```bash
funk --no-oargrid -m free -r grenoble:3,nantes:3,sophia:3,rennes:3,nancy:4 -w 4:00:00 -o "-t deploy"
```

Where the reservation time needs to be adjusted per the requirements of the experiment.

We acquire the list of assigned hosts using the `get_hosts.py` script:

```bash
python python get_hosts.py grenoble,nantes,sophia,rennes,nancy > machines.txt
```

### Step 3: Install Environment using Kadeploy

The environment for the experiment, based on Ubuntu 22.04, is installed on the allocated nodes using Kadeploy. Execute the following command:

```bash
kadeploy3 -M -f machines.txt ubuntu2204-min -k
```

The `-k` option ensures that kadeploy copies the SSH public keys from the frontend account to the root account on each allocated node. The `-M`
option performs a multi-site deploy. With `-f` we can specify the generated machines list.

### Step 4: Configure Inventory

To configure Ansible properly, create an inventory file reflecting the nodes allocated to your experiment. You can view the available nodes
using the `oarstat` command:

```bash
oarstat -u -f
```

A script _generate_config.py_ is provided that generates _inventory/hosts_ and _job/job\*.json_ based on a _machines.json_ file. The machines JSON
file needs to be populated with the reserved nodes.

### Step 5: Configure using Ansible

Utilize Ansible to configure the allocated nodes with the necessary software and settings by executing the following playbook:

```bash
ansible-playbook -i inventory/hosts playbook.yml
```

### Step 6: Create jobs

### Step 7: Download the InfluxDB bucket

The InfluxDB bucket can be backed up for further analysis:

```bash
./influx backup -b bucket -t bzO10KmR8x ~/bucket_$(date '+%Y-%m-%d_%H-%M')
```

And be restored as follows:

```bash
influx restore -o org -t bzO10KmR8x bucket_file
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

### Accessing InfluxDB

To access the deployed InfluxDB, SSH (dynamic) tunneling can be used. Ensure that a
[SSH jump host](https://www.grid5000.fr/w/Getting_Started#Recommended_tips_and_tricks_for_an_efficient_use_of_Grid.275000) has been configured
to access the appropriate Grid'5000 site. Then, set up SSH tunneling to the appropriate site:

```bash
ssh -D 9090 -N site.g5k
```

To browse configure to the InfluxDB web frontend, configure a web browser to use the created SSH tunnel. For instance, for Google Chrome on
Linux, one can start Google Chrome as follows:

```bash
google-chrome-stable --proxy-server=socks://localhost:9090
```
