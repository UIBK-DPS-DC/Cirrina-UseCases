import requests
import argparse

HEADERS = {
    "Content-Type": "application/json",
    "Accept": "application/json"
}
INSTANCES = 1

if __name__ == "__main__":

    parser = argparse.ArgumentParser()

    parser.add_argument("--host", type=str, required=False, 
                        default="http://localhost:8080")
    
    parser.add_argument("--workflows", type=str, required=False, 
                        default="detector,surveillance")
    
    parser.add_argument("--key", type=str, required=False, 
                        default="instance")

    args = parser.parse_args()

    workflows = str(args.workflows).split(",")
    print(f"Workflows: {workflows}")

    # Start the selected workflows assigning the same business key to each of them
    # This allows all workflows to communicate event based    
    for workflow in workflows:
        for i in range(INSTANCES):
            url = f"{args.host}/{workflow}?businessKey={args.key}{i}"

            # Start workflow
            response = requests.post(url, headers=HEADERS)

            print(f"Workflow: {workflow}, Status Code: {response.status_code}, Response:\n{response.text}")

            if response.status_code not in (200, 201):
                print("Error starting workflow! Exiting...")
                exit(1)