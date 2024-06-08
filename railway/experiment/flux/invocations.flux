// Gather the invocations, defined as the number of invocations made, over the last 12 hours
// Resolution: 1 minute
from(bucket: "bucket")
    |> range(start: -12)
    |> filter(fn: (r) => r["_measurement"] == "cirrina.invocations")
    |> filter(fn: (r) => r["_field"] == "counter")
    |> aggregateWindow(every: 1m, fn: mean, createEmpty: false)
    |> group(columns: ["_time"])
    |> sum(column: "_value")
    |> group()
    |> difference()