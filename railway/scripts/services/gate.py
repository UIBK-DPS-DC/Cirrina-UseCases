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

gate_response_time_gauge = meter.create_gauge("gate_response_time")

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

    # Train is coming, capture response time
    if gate_statuses[id].status == "up":
        t = time.time()
        dt = t - gate_statuses[id].last

        gate_response_time_gauge.set(dt)

        gate_statuses[id].last = t

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
