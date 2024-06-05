// Gather the response time, defined as the time in between light on/gate up, over the last 12 hours.
data =
    from(bucket: "bucket")
        |> range(start: -12h)
        |> filter(fn: (r) => r["_measurement"] == "gate_status" or r["_measurement"] == "light_status")
        |> filter(fn: (r) => r["_field"] == "gauge")
        |> sort(columns: ["_time"])
        |> map(fn: (r) => ({r with _prev_value: r._value}), mergeKey: true)
        |> group()
        |> difference(columns: ["_value"], keepFirst: true)
        |> filter(fn: (r) => r._value == 1)
        |> elapsed(unit: 1s)
        |> yield(name: "response_time")