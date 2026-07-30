#!/usr/bin/env bash
# Tears down the whole local kind cluster and its containers.
set -euo pipefail
kind delete cluster --name vehicle-rental
