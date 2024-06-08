import argparse
import json

import commentjson

from kazoo.client import KazooClient

if __name__ == "__main__":
    parser = argparse.ArgumentParser()

    parser.add_argument(
        "--host", type=str, default="127.0.0.1:2181", help="ZooKeeper host address"
    )

    parser.add_argument(
        "--csml_file", type=str, required=True, help="Path to the CSML file"
    )
    parser.add_argument(
        "--services_file",
        type=str,
        required=True,
        help="Path to the service descriptions JSON file",
    )

    parser.add_argument(
        "--local_data",
        type=json.loads,
        default="{}",
        help="Local data map in JSON format",
    )

    parser.add_argument(
        "--instances",
        type=int,
        default=1,
        help="Amount of instances for each state machine",
    )

    args = parser.parse_args()

    print(f"ZooKeeper Host     : {args.host}")
    print(f"CSML File Path     : {args.csml_file}")
    print(f"Services File Path : {args.services_file}")
    print(f"Local Data Map     : {args.local_data}")
    print(f"# Instances        : {args.instances}")

    # Load the csm file and service descriptions file (removes comments)
    with open(args.csml_file) as csml_file:
        csm = commentjson.load(csml_file)
        print(f"Successfully read CSML file: {csml_file.name}")

    sm_names = list(
        map(
            lambda sm: sm["name"],
            filter(
                lambda sm: "abstract" not in sm or not sm["abstract"],
                csm["stateMachines"],
            ),
        )
    )

    with open(args.services_file) as services_file:
        services = commentjson.load(services_file)
        print(f"Successfully read service descriptions file: {services_file.name}")

    # Creates a job description for a provided state machine
    def create_job_description(state_machine_name: str) -> dict:
        return {
            "serviceImplementations": services["serviceImplementations"],
            "collaborativeStateMachine": csm,
            "stateMachineName": state_machine_name,
            "localData": (
                args.local_data[state_machine_name]
                if state_machine_name in args.local_data
                else {}
            ),
            "bindEventInstanceIds": [],
            "runtimeName": "runtime"
        }

    # Create the KazooClient using the provided host
    zk = KazooClient(hosts=args.host)

    try:
        # Start the Kazoo Client
        zk.start()

        # Ensure the /jobs node exists, create if necessary
        zk.ensure_path("/jobs")

        for i in range(args.instances):
            for job_num, sm_name in enumerate(sm_names):
                node_path: str = f"/jobs/job{job_num+1 + i * len(sm_names)}"

                # Delete the current node if it already exists
                if zk.exists(node_path):
                    print(f"Deleting node '{node_path}'...")
                    zk.delete(node_path)

                # Get the job description for the current state machine
                job_description: dict = create_job_description(sm_name)
                job_description_bytes: bytes = json.dumps(job_description).encode(
                    "utf-8"
                )

                # Create the /jobs/job<i> node
                zk.create(node_path, job_description_bytes)
                print(f"Node '{node_path}' created")
    finally:
        # Stop the client
        zk.stop()
