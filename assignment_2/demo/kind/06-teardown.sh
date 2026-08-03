#!/usr/bin/env bash
# tear down kind cluster
set -euo pipefail
kind delete cluster --name vehicle-rental
