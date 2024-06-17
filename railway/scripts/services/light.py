from fastapi import FastAPI, Request
from pydantic import BaseModel
from starlette.responses import Response
from starlette.status import HTTP_200_OK

import os
import time

app = FastAPI()


class LightStatus(BaseModel):
    status: str
    last: float


light_statuses = {}


@app.post("/light/on")
async def light_on(request: Request):
    # Retrieve the sender ID
    id = request.headers.get("Cirrina-Sender-ID")

    # Create the gate status if not seen before
    if id not in light_statuses:
        light_statuses[id] = LightStatus(status="off", last=time.time())

    # Update its state
    light_statuses[id].status = "on"

    return Response(status_code=HTTP_200_OK)


@app.post("/light/off")
async def light_off(request: Request):
    # Retrieve the sender ID
    id = request.headers.get("Cirrina-Sender-ID")

    # Create the gate status if not seen before
    if id not in light_statuses:
        light_statuses[id] = LightStatus(status="off", last=time.time())

    # Update its state
    light_statuses[id].status = "off"

    return Response(status_code=HTTP_200_OK)
