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
BUSINESS_KEY = "instance1"

if __name__ == "__main__":
    # Start all workflows assigning the same business key to each of them
    # This allows all workflows to communicate event based
    for workflow in WORKFLOWS:
        url = f"{BASE_URL}{workflow}?businessKey={BUSINESS_KEY}"

        response = requests.post(url, headers=HEADERS)
        
        print(f"Workflow: {workflow}, Status Code: {response.status_code}, Response:\n{response.text}")