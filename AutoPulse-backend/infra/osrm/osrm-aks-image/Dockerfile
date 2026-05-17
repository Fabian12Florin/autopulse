FROM ghcr.io/project-osrm/osrm-backend:v26.5.0-amd64-alpine

WORKDIR /data

COPY osrm-data/ /data/

EXPOSE 5000

CMD ["osrm-routed", "--algorithm", "mld", "--port", "5000", "--ip", "0.0.0.0", "/data/romania-latest.osrm"]
