// Gather the throughput, defined as the number of handled peripheral events, over the last 12 hours
// Resolution: 1 minute
from(bucket: "bucket")
    |> range(start: -12)
    |> filter(fn: (r) => r["_measurement"] == "cirrina.events.handled")
    |> filter(fn: (r) => r["_field"] == "counter")
    |> filter(fn: (r) => r["cirrina.event.channel"] == "peripheral")
    |> aggregateWindow(every: 1m, fn: mean, createEmpty: false)
    |> group(columns: ["_time"])
    |> sum(column: "_value")
    |> group()
    |> difference()