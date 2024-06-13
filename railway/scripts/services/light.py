from fastapi import FastAPI, Request
from pydantic import BaseModel
from starlette.responses import Response
from starlette.status import HTTP_200_OK

from opentelemetry import metrics
from opentelemetry.sdk.metrics import MeterProvider
from opentelemetry.sdk.metrics.export import PeriodicExportingMetricReader
from opentelemetry.exporter.otlp.proto.grpc.metric_exporter import OTLPMetricExporter
from opentelemetry.sdk.resources import SERVICE_NAME, Resource

import os
import time

resource = Resource(attributes={SERVICE_NAME: "railway-simulation"})

exporter = OTLPMetricExporter(endpoint=os.environ["OTLP_ENDPOINT"])

metric_reader = PeriodicExportingMetricReader(
    exporter, export_interval_millis=int(os.environ["METRICS_INTERVAL"])
)

provider = MeterProvider(resource=resource, metric_readers=[metric_reader])

metrics.set_meter_provider(provider)
meter = metrics.get_meter("railway")

light_response_time_gauge = meter.create_gauge("light_response_time")

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

    # Train is coming, capture response time
    if light_statuses[id].status == "off":
        t = time.time()
        dt = t - light_statuses[id].last

        light_response_time_gauge.set(dt)

        light_statuses[id].last = t

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
