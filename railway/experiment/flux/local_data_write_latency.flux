// Gather the data write latency, defined as the assignment latency, of local data over the last 12 hours.
// Resolution: 1 minute
from(bucket: "bucket")
    |> range(start: -12h)
    |> filter(fn: (r) => r["_measurement"] == "cirrina.action.data_latency_ms")
    |> filter(fn: (r) => r["_field"] == "gauge")
    |> filter(fn: (r) => r["cirrina.data.locality"] == "local")
    |> filter(fn: (r) => r["cirrina.data.operation"] == "assign")
    |> group(columns: ["cirrina.data.operation", "cirrina.data.size"])
    |> aggregateWindow(every: 1m, fn: mean, createEmpty: false)
    |> yield(name: "local_data_write_latency")