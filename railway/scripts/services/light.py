from fastapi import FastAPI
from pydantic import BaseModel
from starlette.responses import Response
from starlette.status import HTTP_200_OK

app = FastAPI()


class LightStatus(BaseModel):
    status: str


light_status = LightStatus(status="off")


@app.get("/light/status")
def get_light_status():
    return light_status


@app.post("/light/on")
def light_on():
    light_status.status = "on"
    return Response(status_code=HTTP_200_OK)


@app.post("/light/off")
def light_off():
    light_status.status = "off"
    return Response(status_code=HTTP_200_OK)
