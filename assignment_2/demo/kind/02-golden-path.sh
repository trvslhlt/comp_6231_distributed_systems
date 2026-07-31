#!/usr/bin/env bash
# golden path
# - demonstrates
#   - successful request handling
#   - load balancing to APIs
set -euo pipefail

URL="http://localhost:8090/total?vehicleType=SUV&season=Summer&days=2"

for i in 1 2 3 4; do
  echo "--- request $i: GET $URL ---"
  curl -s "$URL" \
    | python3 -c "import json,sys; r=json.load(sys.stdin); print('data:', json.dumps(r['data'])); d=r['debug']; print('servedBy:', d['servedByInstanceId'], '| upstream:', d['upstreamInstanceId'])"
  echo
done
