#!/usr/bin/env bash
# Scenario 1 (kind): golden path + load balancing across N instances.
# Same as the compose version, but against the single client-facing entrypoint — no need to
# check two ports, since the Service only ever has the active pod as an endpoint.
# Assumes port-forward.sh is running in another terminal.
set -euo pipefail

for i in 1 2 3 4; do
  curl -s "http://localhost:8090/total?vehicleType=SUV&season=Summer&days=2" \
    | python3 -c "import json,sys; d=json.load(sys.stdin)['debug']; print('servedBy:', d['servedByInstanceId'], '| upstream:', d['upstreamInstanceId'])"
done
