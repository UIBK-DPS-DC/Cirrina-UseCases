import Event_pb2

import asyncio
import uuid
import nats
import argparse
import sys

TRAIN_SPEED_IN_MS = 33.3
TRAIN_LENGTH_IN_M = 150.0

TICK_RATE_IN_S = 0.01

SENSOR_POSITIONS = [0.0, 200.0, 400.0]

TRAINS_INTERVAL_IN_S = 30.0

TIME_FACTOR = 1.0


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
        print("\033[H\033[J", end="")
        print(f"Simulated time: {self._simulated_time_in_s}")
        print(f"Next arrival: {self._next_arrival_time_in_s}")
        print(f"Num. trains: {len(self._trains)}")

        for i, train in enumerate(self._trains):
            print(f"Train {i} at: {train.front_position()}")

        s = False

        print("Sensor values:")
        for i, sensor_value in enumerate(self._sensor_values):
            print(f"{i}: {sensor_value}")

            s = s or sensor_value

        subject = "peripheral.sensor"

        # Specify event data
        event = Event_pb2.Event()

        event.id = str(uuid.uuid4())
        event.name = "sensor"
        event.channel = Event_pb2.Event.PERIPHERAL

        # Specify variable data
        variable = event.data.add()

        variable.name = "value"
        variable.value.bool = s

        # Publish event
        await self._nc.publish(subject, event.SerializeToString())

        print(event)


async def main():
    parser = argparse.ArgumentParser(prog=sys.argv[0])

    parser.add_argument("nats_url")

    args = parser.parse_args()

    nc = await nats.connect(args.nats_url)

    simulation = Simulation(TRAINS_INTERVAL_IN_S, SENSOR_POSITIONS, TIME_FACTOR, nc)
    await simulation.simulate()

    await nc.drain()


if __name__ == "__main__":
    asyncio.run(main())
