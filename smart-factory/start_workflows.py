# Usage ALL workflows: python start_workflows.py --host "http://localhost:8080" --workflows "job_control,robotic_arm,conveyor_belt,camera_sensor,photoelectric_sensor_start,photoelectric_sensor_end" --key "i1"
import requests
import argparse

HEADERS = {
    "Content-Type": "application/json",
    "Accept": "application/json"
}
INSTANCES = 1

WORKFLOW_INPUT = {
    "job_control": {
        "totalProducts": 100
    },
    "robotic_arm": {
        "partsPerProduct": 10
    }
}

if __name__ == "__main__":

    parser = argparse.ArgumentParser()

    parser.add_argument("--host", type=str, required=False, default="http://localhost:8080/")
    parser.add_argument("--workflows", type=str, required=True)
    parser.add_argument("--key", type=str, required=True)

    args = parser.parse_args()

    workflows = str(args.workflows).split(",")
    print(f"Workflows: {workflows}")

    # Start the selected workflows assigning the same business key to each of them
    # This allows all workflows to communicate event based    
    for workflow in workflows:
        for i in range(INSTANCES):
            url = f"{args.host}/{workflow}?businessKey={args.key}{'' if i == 0 else i}"

            input = WORKFLOW_INPUT[workflow] \
                if workflow in WORKFLOW_INPUT \
                else {}

            # Start workflow
            response = requests.post(url, headers=HEADERS, json=input)

            print(f"Workflow: {workflow}, Status Code: {response.status_code}, Response:\n{response.text}")

            if response.status_code not in (200, 201):
                print("Error starting workflow! Exiting...")
                exit(1)