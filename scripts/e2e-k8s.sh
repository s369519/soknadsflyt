#!/usr/bin/env bash
set -euo pipefail

NS=soknadsflyt
PF1=""
PF2=""
cleanup() { kill $PF1 $PF2 2>/dev/null || true; }
trap cleanup EXIT

kubectl port-forward -n "$NS" svc/intake-service 8095:8080 >/dev/null 2>&1 &
PF1=$!
kubectl port-forward -n "$NS" svc/behandling-service 8096:8080 >/dev/null 2>&1 &
PF2=$!

# wait_for <url> <forventet tekst> <maks sekunder>
wait_for() {
  local url=$1 forventet=$2 maks=$3
  for _ in $(seq 1 "$maks"); do
    if curl -sf "$url" | grep -q "$forventet"; then
      return 0
    fi
    sleep 1
  done
  echo "FEIL: fikk ikke $forventet fra $url innen ${maks}s" >&2
  return 1
}

echo "1) Helsesjekk"
wait_for localhost:8095/actuator/health '"UP"' 60
wait_for localhost:8096/actuator/health '"UP"' 60

echo "2) Ugyldig søknad skal gi 400"
kode=$(curl -s -o /dev/null -w "%{http_code}" -X POST localhost:8095/soknader \
  -H 'Content-Type: application/json' \
  -d '{"sokerNavn":"Kari","sokerEpost":"ikke-en-epost","type":"DAGPENGER"}')
if [ "$kode" != "400" ]; then
  echo "FEIL: forventet 400, fikk $kode" >&2
  exit 1
fi

echo "3) Gyldig søknad"
svar=$(curl -sf -X POST localhost:8095/soknader \
  -H 'Content-Type: application/json' \
  -d '{"sokerNavn":"Kari Nordmann","sokerEpost":"kari@example.com","type":"DAGPENGER"}')
id=$(echo "$svar" | sed -E 's/.*"id":"([^"]+)".*/\1/')
if [ -z "$id" ] || [ "$id" = "$svar" ]; then
  echo "FEIL: fant ingen id i svaret: $svar" >&2
  exit 1
fi
echo "id: $id"

echo "4) Søknaden skal bli FERDIG hos intake"
wait_for "localhost:8095/soknader/$id" '"FERDIG"' 60

echo "5) Behandlingen skal være FERDIG hos behandling"
wait_for "localhost:8096/behandlinger/$id" '"FERDIG"' 60

echo "OK: hele flyten fungerer"