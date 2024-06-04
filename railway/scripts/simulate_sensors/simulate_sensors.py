import Event_pb2

import asyncio
import nats

from opentelemetry import metrics
from opentelemetry.sdk.metrics import MeterProvider

import uuid
import os
import time

TRAIN_SPEED_IN_MS = 33.3
TRAIN_LENGTH_IN_M = 150.0

TICK_RATE_IN_S = 0.01

SENSOR_POSITIONS = [0.0, 200.0, 400.0]

TRAINS_INTERVAL_IN_S = 30.0

TIME_FACTOR = 1.0

provider = MeterProvider()

metrics.set_meter_provider(provider)

meter = metrics.get_meter("railway")

events_published_counter = meter.create_counter("events_published")


class Train:
    def __init__(self):
        self._position = 0.0
        self._speed_in_ms = TRAIN_SPEED_IN_MS
        self._length = TRAIN_LENGTH_IN_M

    def update_position(self, delta_time):
        self._position += delta_time * self._speed_in_ms

    def front_position(self):
        return self._position

    def back_position(self):
        return self._position - self._length


class Simulation:
    def __init__(self, trains_interval_in_s, sensor_positions, time_factor, nc):
        self._trains = []

        self._simulated_time_in_s = 0.0
        self._last_simulation_time_in_s = self._simulated_time_in_s

        self._trains_interval_in_s = trains_interval_in_s
        self._next_arrival_time_in_s = 0.0

        self._sensor_positions = sensor_positions
        self._sensor_values = [False for _ in self._sensor_positions]

        self._time_factor = time_factor

        self._nc = nc

    def _new_train(self):
        train = Train()
        self._trains.append(train)

    def _update_sensor_values(self):
        for i, sensor_position in enumerate(self._sensor_positions):
            self._sensor_values[i] = any(
                train.back_position() <= sensor_position < train.front_position()
                for train in self._trains
            )

    async def simulate(self):
        while True:
            current_simulation_time = self._simulated_time_in_s
            delta_simulation_time = TICK_RATE_IN_S * self._time_factor

            self._last_simulation_time_in_s = current_simulation_time
            self._simulated_time_in_s += delta_simulation_time

            # Spawn new trains
            if self._simulated_time_in_s >= self._next_arrival_time_in_s:
                self._new_train()
                self._next_arrival_time_in_s += self._trains_interval_in_s

            # Update train positions
            for train in self._trains:
                train.update_position(delta_simulation_time)

            # Remove trains beyond the kill point
            self._trains = [
                train
                for train in self._trains
                if train.back_position() < self._sensor_positions[-1]
            ]

            # Update sensor values
            self._update_sensor_values()

            # Broadcast sensor values
            await self._broadcast_sensor_values()

            # Sleep to maintain tick rate
            await asyncio.sleep(TICK_RATE_IN_S)

    async def _broadcast_sensor_values(self):
        s = False

        for i, sensor_value in enumerate(self._sensor_values):
            s = s or sensor_value

        subject = "peripheral.sensor"

        # Specify event data
        event = Event_pb2.Event()

        event.createdTime = time.time_ns() / 1.0e6
        event.id = str(uuid.uuid4())
        event.name = "sensor"
        event.channel = Event_pb2.Event.PERIPHERAL

        # Specify variable data
        variable = event.data.add()

        variable.name = "value"
        variable.value.bool = s

        # Publish event
        await self._nc.publish(subject, event.SerializeToString())

        events_published_counter.add(1)


async def main():
    nats_url = os.environ["NATS_URL"]

    nc = await nats.connect(nats_url)

    print(f"Running sensor simulation, NATS URL={nats_url}")

    simulation = Simulation(TRAINS_INTERVAL_IN_S, SENSOR_POSITIONS, TIME_FACTOR, nc)
    await simulation.simulate()

    await nc.drain()


if __name__ == "__main__":
    asyncio.run(main())
