import requests

BASE_URL = "http://localhost:8080/"
HEADERS = {
    "Content-Type": "application/json",
    "Accept": "application/json"
}
WORKFLOWS = [
    "job_control",
    "robotic_arm",
    "conveyor_belt",
    "camera_sensor",
    "photoelectric_sensor_start",
    "photoelectric_sensor_end"
]
BUSINESS_KEY = "instance"
INSTANCES = 1

WORKFLOW_INPUT = {
    "job_control": {
        "totalProducts": 300
    },
    "robotic_arm": {
        "partsPerProduct": 10
    }
}

if __name__ == "__main__":
    # Start all workflows assigning the same business key to each of them
    # This allows all workflows to communicate event based    
    for workflow in WORKFLOWS:
        for i in range(INSTANCES):
            url = f"{BASE_URL}{workflow}?businessKey={BUSINESS_KEY}{'' if i == 0 else i}"

            input = WORKFLOW_INPUT[workflow] \
                if workflow in WORKFLOW_INPUT \
                else {}

            # Start workflow
            response = requests.post(url, headers=HEADERS, json=input)

            print(f"Workflow: {workflow}, Status Code: {response.status_code}, Response:\n{response.text}")

            if response.status_code not in (200, 201):
                print("Error starting workflow! Exiting...")
                break