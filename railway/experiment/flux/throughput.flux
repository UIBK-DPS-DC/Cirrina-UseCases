// Gather the throughput, defined as the number of handled peripheral events, over the last 12 hours
// Resolution: 1 minute
throughput =
    from(bucket: "bucket")
        |> range(start: -12)
        |> filter(fn: (r) => r["_measurement"] == "cirrina.events.handled")
        |> filter(fn: (r) => r["_field"] == "counter")
        |> filter(fn: (r) => r["cirrina.event.channel"] == "peripheral")
        |> aggregateWindow(every: 1m, fn: mean, createEmpty: false)
        |> difference()

// Gather the number of published events over the last 12 hours
// Resolution: 1 minute
published =
    from(bucket: "bucket")
        |> range(start: -12)
        |> filter(fn: (r) => r["_measurement"] == "events_published")
        |> filter(fn: (r) => r["_field"] == "counter")
        |> aggregateWindow(every: 1m, fn: mean, createEmpty: false)
        |> difference()

// Combine both tables
union(tables: [throughput, published])