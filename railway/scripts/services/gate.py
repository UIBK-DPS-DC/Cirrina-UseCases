from fastapi import FastAPI, Request
from pydantic import BaseModel
from starlette.responses import Response
from starlette.status import HTTP_200_OK

import time

app = FastAPI()


class GateStatus(BaseModel):
    status: str
    last: float


gate_statuses = {}


@app.post("/gate/down")
def gate_down(request: Request):
    # Retrieve the sender ID
    id = request.headers.get("Cirrina-Sender-ID")

    # Create the gate status if not seen before
    if id not in gate_statuses:
        gate_statuses[id] = GateStatus(status="up", last=time.time())

    # Update its state
    gate_statuses[id].status = "down"

    return Response(status_code=HTTP_200_OK)


@app.post("/gate/up")
def gate_up(request: Request):
    # Retrieve the sender ID
    id = request.headers.get("Cirrina-Sender-ID")

    # Create the gate status if not seen before
    if id not in gate_statuses:
        gate_statuses[id] = GateStatus(status="up", last=time.time())

    # Update its state
    gate_statuses[id].status = "up"

    return Response(status_code=HTTP_200_OK)
