// Gather the gate response time, defined as the time in between gate up, over the last 12 hours.
// Resolution: 1 minute
gate_response_time =
    from(bucket: "bucket")
        |> range(start: -12h)
        |> filter(fn: (r) => r["_measurement"] == "gate_response_time")
        |> filter(fn: (r) => r["_field"] == "gauge")
        |> group(columns: ["_measurement", "_field", "host"])
        |> aggregateWindow(every: 1m, fn: mean, createEmpty: false)

// Gather the gate response time, defined as the time in between light on, over the last 12 hours.
// Resolution: 1 minute
light_response_time =
    from(bucket: "bucket")
        |> range(start: -12h)
        |> filter(fn: (r) => r["_measurement"] == "light_response_time")
        |> filter(fn: (r) => r["_field"] == "gauge")
        |> group(columns: ["_measurement", "_field", "host"])
        |> aggregateWindow(every: 1m, fn: mean, createEmpty: false)

// Combine both tables
union(tables: [gate_response_time, light_response_time])