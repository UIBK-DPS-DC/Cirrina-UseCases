# Cirrina Railway Experiment

## Grid5000 Setup

The following instructions imply that the [Grid'5000 Getting Started Page](https://www.grid5000.fr/w/Getting_Started)
has been read and understood. Determining the right location to execute the commands below is implied from an understanding
of the Grid'5000 environment.

### Step 1: Install Ansible

Make sure to install Ansible as follows:

```bash
export PATH=$HOME/.local/bin:$PATH
pip install --user ansible
```

### Step 2: Request resources

### Step 3: Install environment using Kadeploy

The environment (Ubuntu 22.04) is installed using [Kadeploy]() as follows:

```bash
kadeploy3 ubuntu2204-min -k
```

The _-k_ option ensures that kadeploy copies the contents of authorized_keys from the frontend account to the root account on
each of your nodes.

### Step 4: Configure inventory

The Ansible inventory needs to be set up according to the requested resources, to view the nodes available use `oarstat`:

```bash
oarstat -u -f
```

### Step 5: Configure using Ansible

Next, Ansible is used to configure the resources.

```bash
ansible-playbook -i inventory/hosts playbook.yml
```

### Benchmarking NATS

NATS can be benchmarked as follows from the host that runs a NATS server:

```bash
nats -s nats://localhost:4222 bench test --pub 1 --size 16 --msgs 10000000
```
