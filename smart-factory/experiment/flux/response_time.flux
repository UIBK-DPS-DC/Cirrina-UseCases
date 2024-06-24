// Gather the gate response time, defined as the time in between gate up, over the last 12 hours.
// Resolution: 1 minute
from(bucket: "bucket")
    |> range(start: -12h)
    |> filter(fn: (r) => r["_measurement"] == "gate_response_time" or r["_measurement"] == "light_response_time")
    |> filter(fn: (r) => r["_field"] == "gauge")
    |> aggregateWindow(every: 1m, fn: mean, createEmpty: false)