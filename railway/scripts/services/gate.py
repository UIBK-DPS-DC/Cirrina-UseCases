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

gate_response_time_gauge = meter.create_gauge("gate_response_time")

app = FastAPI()


class GateStatus(BaseModel):
    status: str


gate_status = GateStatus(status="up")


@app.get("/gate/status")
def get_gate_status():
    return gate_status


last = time.time()


@app.post("/gate/down")
def gate_down():
    global last

    if gate_status.status == "up":
        t = time.time()
        dt = t - last

        gate_response_time_gauge.set(dt)

        last = t

    gate_status.status = "down"
    return Response(status_code=HTTP_200_OK)


@app.post("/gate/up")
def gate_up():
    gate_status.status = "up"
    return Response(status_code=HTTP_200_OK)
