#!/usr/bin/env bash
set -u

pkill -f "kubectl port-forward" 2>/dev/null
kubectl port-forward -n soknadsflyt svc/intake-service 8095:8080 >/dev/null 2>&1 &
PF1=$!
kubectl port-forward -n soknadsflyt svc/behandling-service 8096:8080 >/dev/null 2>&1 &
PF2=$!
trap 'kill $PF1 $PF2 2>/dev/null' EXIT
sleep 3

echo "1) Helsesjekk"
curl -s localhost:8095/actuator/health; echo
curl -s localhost:8096/actuator/health; echo

echo "2) Ugyldig søknad skal gi 400"
curl -s -o /dev/null -w "%{http_code}\n" -X POST localhost:8095/soknader \
  -H 'Content-Type: application/json' \
  -d '{"sokerNavn":"Kari","sokerEpost":"ikke-en-epost","type":"DAGPENGER"}'

echo "3) Gyldig søknad"
ID=$(curl -s -X POST localhost:8095/soknader -H 'Content-Type: application/json' \
  -d '{"sokerNavn":"Kari Nordmann","sokerEpost":"kari@example.com","type":"DAGPENGER"}' \
  | sed -E 's/.*"id":"([^"]+)".*/\1/')
echo "id: $ID"

sleep 6

echo "4) Status hos intake (forventer FERDIG)"
curl -s localhost:8095/soknader/$ID; echo

echo "5) Status hos behandling (forventer FERDIG)"
curl -s localhost:8096/behandlinger/$ID; echo