import json
import sys

from requests import post, get
from time import sleep
from uuid import uuid4, UUID

RUNTIME_URL = "http://localhost:43962"

START_SM_URL = f"{RUNTIME_URL}/start"
PS_URL = f"{RUNTIME_URL}/ps"
STOP_SM_URL = f"{RUNTIME_URL}/stop"
NATS_URL = "nats://localhost:4222"

SM_NAMES = ["jobControlSystem", "smsProcessor", "eMailProcessor", "logProcessor", "roboticArmSystem", "monitoringSystem", "conveyorBeltSystem"]
LOCAL_DATA = {
    "jobControlSystem": {
        "totalProducts": "2"      # Real world scenario would be much higher
    },
    "roboticArmSystem": {
        "partsPerProduct": "2"
    }
}

def start_sm(instance_id: str, sm_name: str, csm_desc: any) -> bool:
    try:
        response = post(
            START_SM_URL,
            json={
                "instance_id": instance_id,
                "state_machine_name": sm_name,
                "event_queue_url": NATS_URL,
                "persistent_context_url": NATS_URL,
                "csm_description": csm_desc,
                "local_data": LOCAL_DATA[sm_name] if sm_name in LOCAL_DATA else {},
                "local_service_routing": {},
                "remote_service_routing": {},
                "runtime_ip_address": RUNTIME_URL,
                "bind_events": []
            }
        )
    except Exception as e:
        print(f"Starting {sm_name} threw an error:\n{e}")
        return False

    if response.status_code == 200:
        print(f"{sm_name} successfully started ({instance_id})")
        return True
    else:
        print(f"Starting {sm_name} failed:\n{response.status_code}: {response.text}")
        return False


if __name__ == "__main__":

    if len(sys.argv) <= 1:
        print(f"Usage: {sys.argv[0]} <csm_file_name>")
        exit(1)

    CSM_PATH = f"csm/{sys.argv[1]}"
    RUN_N = 1

    with open(CSM_PATH) as f:
        csm = json.loads(f.read())

    csm_ready: bool = True
    active_sms: dict[str, str] = {}

    for j in range(RUN_N):
        for i, name in enumerate(SM_NAMES):
            ident: str = str(uuid4()).upper()
            if not start_sm(ident, name, csm):
                csm_ready = False
                break
            else:
                active_sms[ident] = name

    while csm_ready:
        try:
            sleep(60)

            ps_response = get(PS_URL)
            instance_ids = ps_response.json()["instance_ids"]

            print("-" * 60, "PS:", sep="\n")
            alive: int = 0
            for uuid, name in active_sms.items():
                if uuid in instance_ids:
                    print(name, f"({uuid})")
                    alive += 1
                else:
                    print(f"SM {name} is no longer alive")
            if alive == 0:
                break
        except KeyboardInterrupt as r:
            print("Stopping all state machines")
            for uuid, name in active_sms.items():
                print(f"Stopping {name} ...")
                post(f"{STOP_SM_URL}/{uuid}")
            break
        except Exception as e:
            print(e)
            break
