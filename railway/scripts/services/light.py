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

resource = Resource(attributes={SERVICE_NAME: "railway-simulation"})

exporter = OTLPMetricExporter(endpoint=os.environ["OTLP_ENPOINT"])

metric_reader = PeriodicExportingMetricReader(
    exporter, export_interval_millis=int(os.environ["METRICS_INTERVAL"])
)

provider = MeterProvider(resource=resource, metric_readers=[metric_reader])

metrics.set_meter_provider(provider)
meter = metrics.get_meter("railway")

light_status_counter = meter.create_gauge("gate_status")

app = FastAPI()


class LightStatus(BaseModel):
    status: str


light_status = LightStatus(status="off")


@app.get("/light/status")
def get_light_status():
    return light_status


@app.post("/light/on")
def light_on():
    light_status_counter.set(1)

    light_status.status = "on"
    return Response(status_code=HTTP_200_OK)


@app.post("/light/off")
def light_off():
    light_status_counter.set(0)

    light_status.status = "off"
    return Response(status_code=HTTP_200_OK)
