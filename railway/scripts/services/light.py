from fastapi import FastAPI
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


light_status = LightStatus(status="off")


@app.get("/light/status")
def get_light_status():
    return light_status


last = time.time()


@app.post("/light/on")
def light_on():
    global last

    if light_status.status == "off":
        t = time.time()
        dt = t - last

        light_response_time_gauge.set(dt)

        last = t

    light_status.status = "on"
    return Response(status_code=HTTP_200_OK)


@app.post("/light/off")
def light_off():
    light_status.status = "off"
    return Response(status_code=HTTP_200_OK)
