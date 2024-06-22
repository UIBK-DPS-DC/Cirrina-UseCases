import requests

BASE_URL = "http://localhost:8080/"
HEADERS = {
    "Content-Type": "application/json",
    "Accept": "application/json"
}
WORKFLOWS = [
    "person_detector",
    #"face_detector",
    "camera"
]
BUSINESS_KEY = "instance"
INSTANCES = 1

if __name__ == "__main__":
    # Start all workflows assigning the same business key to each of them
    # This allows all workflows to communicate event based    
    for workflow in WORKFLOWS:
        for i in range(INSTANCES):
            url = f"{BASE_URL}{workflow}?businessKey={BUSINESS_KEY}{'' if i == 0 else i}"

            # Start workflow
            response = requests.post(url, headers=HEADERS)

            print(f"Workflow: {workflow}, Status Code: {response.status_code}, Response:\n{response.text}")

            if response.status_code not in (200, 201):
                print("Error starting workflow! Exiting...")
                break