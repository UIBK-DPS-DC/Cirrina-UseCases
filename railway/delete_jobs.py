from kazoo.client import KazooClient

import argparse

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
    finally:
        # Stop the client
        zk.stop()
