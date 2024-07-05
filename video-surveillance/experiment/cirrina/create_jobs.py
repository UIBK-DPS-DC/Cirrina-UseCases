from kazoo.client import KazooClient

import argparse
import os

from pathlib import Path

if __name__ == "__main__":
    parser = argparse.ArgumentParser()

    parser.add_argument(
        "--host", type=str, default="127.0.0.1:2181", help="ZooKeeper host address"
    )

    args = parser.parse_args()

    # Create the KazooClient using the provided host
    zk = KazooClient(hosts=args.host)

    try:
        # Start the Kazoo Client
        zk.start()

        try:
            zk.delete("/jobs", recursive=True)
        except:
            pass

        # Ensure the /jobs node exists, create if necessary
        zk.ensure_path("/jobs")

        job_dir = "job"

        for job in os.listdir(job_dir):
            with open(os.path.join(job_dir, job)) as file:
                job_json = file.read()

            job_json_bytes: bytes = job_json.encode("utf-8")

            job_name = Path(job).stem

            job_path = f"/jobs/{job_name}"

            zk.create(job_path, job_json_bytes)
            print(f"Node '{job_path}' created")
    finally:
        # Stop the client
        zk.stop()
