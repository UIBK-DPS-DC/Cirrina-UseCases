import csv


# Read and parse the log files
def read_log_file(file_path):
    log_data = {}
    with open(file_path, "r") as file:
        reader = csv.reader(file)
        for row in reader:
            hash_value, timestamp = row
            log_data[hash_value] = int(timestamp)
    return log_data


# Calculate the time delta for matching hashes
def calculate_time_deltas(log1, log2):
    deltas = []
    for hash_value, timestamp1 in log1.items():
        if hash_value in log2:
            timestamp2 = log2[hash_value]
            delta = timestamp2 - timestamp1
            deltas.append((hash_value, delta))
    return deltas


# Paths to the log files
log_file_1 = "./send.csv"
log_file_2 = "./analysis.csv"

# Read the log files
log1_data = read_log_file(log_file_1)
log2_data = read_log_file(log_file_2)

# Calculate the time deltas
time_deltas = calculate_time_deltas(log1_data, log2_data)

# Print the results
for hash_value, delta in time_deltas:
    print(f"Hash: {hash_value}, Delta: {delta} ms")
