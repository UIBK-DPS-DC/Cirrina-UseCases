from fastapi import FastAPI
from pydantic import BaseModel
from starlette.responses import Response
from starlette.status import HTTP_200_OK

app = FastAPI()


class GateStatus(BaseModel):
    status: str


gate_status = GateStatus(status="up")


@app.get("/gate/status")
def get_gate_status():
    return gate_status


@app.post("/gate/up")
def gate_up():
    gate_status.status = "up"
    return Response(status_code=HTTP_200_OK)


@app.post("/gate/down")
def gate_down():
    gate_status.status = "down"
    return Response(status_code=HTTP_200_OK)
