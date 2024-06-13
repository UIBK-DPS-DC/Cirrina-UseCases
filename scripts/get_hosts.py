import paramiko
import json
import sys


def ssh_to_site_and_get_oarstat(site):
    hostname = f"{site}"

    client = paramiko.SSHClient()
    client.set_missing_host_key_policy(paramiko.AutoAddPolicy())

    try:
        client.connect(hostname)

        stdin, stdout, stderr = client.exec_command("oarstat -u -J")

        output = stdout.read().decode("utf-8")
        return output
    except Exception as e:
        print(f"Error connecting to {site}: {e}")
    finally:
        client.close()


def get_assigned_network_addresses(data):
    try:
        jobs = json.loads(data)
        addresses = []
        job_ids = []
        for job_id, job_info in jobs.items():
            if "assigned_network_address" in job_info:
                addresses.extend(job_info["assigned_network_address"])
            if "id" in job_info:
                job_ids.append(job_info["id"])
        return (addresses, job_ids)
    except json.JSONDecodeError as e:
        print(f"Error parsing JSON: {e}")
        return []


def main(sites):
    all_hosts = []
    all_job_ids = []

    for site in sites:
        data = ssh_to_site_and_get_oarstat(site)
        if data:
            res = get_assigned_network_addresses(data)
            all_hosts.extend(res[0])
            all_job_ids.extend(res[1])

    for host in all_hosts:
        print(host)

    for job_id in all_job_ids:
        print(job_id, file=sys.stderr)


if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("Usage: get_hosts site1,site2,...")
        sys.exit(1)

    sites_arg = sys.argv[1]
    sites = sites_arg.split(",")

    main(sites)
