#!/usr/bin/env bash
set -euo pipefail

OSRM_IMAGE="ghcr.io/project-osrm/osrm-backend:v26.5.0-amd64-alpine"
OSRM_DATA_DIR="infra/osrm/osrm-data"
PBF_FILE="${OSRM_DATA_DIR}/romania-latest.osm.pbf"
PBF_URL="https://download.geofabrik.de/europe/romania-latest.osm.pbf"

echo "==> Creating OSRM data directory..."
mkdir -p "${OSRM_DATA_DIR}"

echo "==> Removing old generated OSRM files..."
rm -f "${OSRM_DATA_DIR}"/romania-latest.osrm*

echo "==> Downloading latest Romania OSM PBF..."
curl -L -o "${PBF_FILE}" "${PBF_URL}"

echo "==> Resolving Windows path for Docker mount..."
if command -v cygpath >/dev/null 2>&1; then
  WINPWD="$(pwd -W)"
else
  WINPWD="$(pwd)"
fi

echo "==> Running osrm-extract..."
MSYS_NO_PATHCONV=1 docker run --rm -t \
  -v "${WINPWD}/${OSRM_DATA_DIR}:/data" \
  "${OSRM_IMAGE}" \
  osrm-extract -p /opt/car.lua /data/romania-latest.osm.pbf

echo "==> Running osrm-partition..."
MSYS_NO_PATHCONV=1 docker run --rm -t \
  -v "${WINPWD}/${OSRM_DATA_DIR}:/data" \
  "${OSRM_IMAGE}" \
  osrm-partition /data/romania-latest.osrm

echo "==> Running osrm-customize..."
MSYS_NO_PATHCONV=1 docker run --rm -t \
  -v "${WINPWD}/${OSRM_DATA_DIR}:/data" \
  "${OSRM_IMAGE}" \
  osrm-customize /data/romania-latest.osrm

echo "==> Starting OSRM service..."
docker compose up -d osrm

echo "==> OSRM logs:"
docker compose logs -f osrm