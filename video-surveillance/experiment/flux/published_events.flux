// Gather the number of published events over the last 12 hours
// Resolution: 1 minute
from(bucket: "bucket")
    |> range(start: -12)
    |> filter(fn: (r) => r["_measurement"] == "events_published")
    |> filter(fn: (r) => r["_field"] == "counter")
    |> group(columns: ["_measurement", "_field"])
    |> aggregateWindow(every: 1m, fn: mean, createEmpty: false)
    |> difference()