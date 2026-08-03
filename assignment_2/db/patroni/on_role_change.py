#!/usr/bin/env python3
"""Patches this pod's own "role" label whenever Patroni reports a role change, so the
client-facing postgres-primary / postgres-replica Services (which select on that label) route
correctly without any external health-check logic — the same pattern common/k8s/PodRoleLabeler
uses for the load balancer's active/passive routing, just reimplemented here in Python since
this image is Patroni's, not Spring's.

Invoked by Patroni as: on_role_change.py <action> <role> <scope>. Only on_start and
on_role_change carry a role worth reacting to (on_stop does not). Outside Kubernetes (no
POD_NAME/POD_NAMESPACE, e.g. docker-compose) this is a no-op.
"""
import json
import os
import ssl
import sys
import urllib.request

REACTS_TO = ("on_start", "on_role_change")


def main():
    if len(sys.argv) < 3 or sys.argv[1] not in REACTS_TO:
        return

    role = sys.argv[2]
    pod_name = os.environ.get("POD_NAME")
    namespace = os.environ.get("POD_NAMESPACE")
    if not pod_name or not namespace:
        print(f"on_role_change: POD_NAME/POD_NAMESPACE not set (not running in Kubernetes); "
              f"skipping pod label update to role={role}")
        return

    token_path = "/var/run/secrets/kubernetes.io/serviceaccount/token"
    ca_path = "/var/run/secrets/kubernetes.io/serviceaccount/ca.crt"
    with open(token_path) as f:
        token = f.read().strip()

    host = os.environ.get("KUBERNETES_SERVICE_HOST", "kubernetes.default.svc")
    port = os.environ.get("KUBERNETES_SERVICE_PORT", "443")
    url = f"https://{host}:{port}/api/v1/namespaces/{namespace}/pods/{pod_name}"

    body = json.dumps({"metadata": {"labels": {"role": role}}}).encode()
    request = urllib.request.Request(url, data=body, method="PATCH", headers={
        "Authorization": f"Bearer {token}",
        "Content-Type": "application/strategic-merge-patch+json",
    })

    context = ssl.create_default_context(cafile=ca_path)
    try:
        with urllib.request.urlopen(request, context=context, timeout=5) as response:
            print(f"on_role_change: patched pod {namespace}/{pod_name} label role={role} "
                  f"({response.status})")
    except Exception as e:
        print(f"on_role_change: failed to patch pod label to role={role}: {e}", file=sys.stderr)


if __name__ == "__main__":
    main()
