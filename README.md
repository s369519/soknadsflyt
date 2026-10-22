# Søknadsflyt

![CI](https://github.com/s369519/soknadsflyt/actions/workflows/ci.yml/badge.svg)

En liten, hendelsesdrevet integrasjon mellom to Kotlin/Spring Boot-tjenester. En søknad
mottas via REST, behandles asynkront via Kafka, og status flyter tilbake til
mottakstjenesten. Prosjektet kjører lokalt med Docker Compose og i Kubernetes (kind).

## Arkitektur

```mermaid
flowchart LR
  klient[Klient] -->|POST /soknader| intake[intake-service]
  intake --> idb[(PostgreSQL intake)]
  intake -->|soknad.mottatt| kafka{{Kafka}}
  kafka --> behandling[behandling-service]
  behandling --> bdb[(PostgreSQL behandling)]
  behandling -->|GET /api/soknadstyper| regelverk[regelverk-api]
  behandling -->|soknad.behandlet| kafka
  kafka --> intake
```

1. `intake-service` tar imot en søknad (REST), validerer og lagrer den, og publiserer
   `SoknadMottatt` på Kafka.
2. `behandling-service` lytter på hendelsen, henter regler for søknadstypen fra et
   eksternt REST-API, ferdigstiller behandlingen og publiserer `SoknadBehandlet`.
3. `intake-service` lytter på `SoknadBehandlet` og setter søknaden til `FERDIG`.

Det eksterne regelverk-API-et simuleres med WireMock.

## Teknologi

Kotlin, Spring Boot 4, Spring Data JPA, Spring for Apache Kafka, PostgreSQL, Flyway,
Docker, Docker Compose, Kubernetes (kind, Kustomize), Testcontainers, GitHub Actions.

## Kjør lokalt med Docker Compose

Krever Docker og JDK 21.

```bash
docker compose up -d
cd intake-service && ./gradlew bootRun        # port 8080
cd behandling-service && ./gradlew bootRun    # port 8081
```

Send en søknad og følg den:

```bash
curl -X POST localhost:8080/soknader -H 'Content-Type: application/json' \
  -d '{"sokerNavn":"Kari Nordmann","sokerEpost":"kari@example.com","type":"DAGPENGER"}'
curl localhost:8080/soknader/<id>
curl localhost:8081/behandlinger/<id>
```

Søknaden går fra `MOTTATT` til `FERDIG` hos intake, og behandlingen står som `FERDIG`
hos behandling.

## Kjør i Kubernetes (lokalt med kind)

Krever Docker, [kind](https://kind.sigs.k8s.io/) og kubectl.

```bash
kind create cluster --name soknadsflyt

docker build -t intake-service:local intake-service
docker build -t behandling-service:local behandling-service
kind load docker-image intake-service:local behandling-service:local --name soknadsflyt

kubectl apply -k k8s/overlays/local
kubectl get pods -n soknadsflyt -w
```

Når alle pods er `Running`, åpne port-forward i to terminalvinduer:

```bash
kubectl port-forward -n soknadsflyt svc/intake-service 8095:8080
kubectl port-forward -n soknadsflyt svc/behandling-service 8096:8080
```

Send så en søknad som over, men mot `localhost:8095` (intake) og `localhost:8096`
(behandling).

Manifestene bruker Kustomize:

- `k8s/base` inneholder det som er felles: Deployments for tjenestene, StatefulSets for
  Postgres, Kafka, mock-API, ConfigMaps og liveness-/readiness-probes mot Spring Actuator.
- `k8s/overlays/local` tilpasser det til en lokal kind-klynge: bildetagger og et lokalt
  testpassord som Kustomize genererer som en Secret.
- `k8s/overlays/scaled` kjører to replikaer av hver tjeneste (`kubectl apply -k k8s/overlays/scaled`).
  Siden `soknad.mottatt` har én partisjon, er det bare én replika av `behandling-service`
  som får meldinger. Resten venter som reserve.

Fjern klyngen med `kind delete cluster --name soknadsflyt`.

## Tester

Integrasjonstestene bruker Testcontainers (ekte PostgreSQL og Kafka) og krever Docker:

```bash
cd intake-service && ./gradlew test
cd behandling-service && ./gradlew test
```

Testene dekker blant annet at en lagret søknad gir en hendelse på Kafka, at validering
avviser ugyldige søknader, at `SoknadBehandlet` setter søknaden til `FERDIG`, og at samme
melding levert to ganger ikke gir dobbel behandling.

GitHub Actions kjører bygg og tester for begge tjenestene på hver push og pull request.

## Designvalg

- **Hver tjeneste har sin egen database.** Tjenestene deler ingen tabeller og er
  uavhengige av hverandre. Kontrakten mellom dem er JSON-meldingene, og hver tjeneste har
  sin egen kopi av hendelsesklassene i stedet for en felles modul.
- **Hendelsen publiseres etter commit.** Det hindrer at `SoknadMottatt` sendes for en
  søknad som aldri ble lagret.
- **Idempotent konsument.** Kafka leverer minst én gang, så `behandling-service` kan få
  samme melding flere ganger uten å behandle søknaden flere ganger.
- **Hver tjeneste oppretter topicene den bruker.** Da kan en consumer starte uten å
  vente på at en annen tjeneste har opprettet topicet.
- **Konfigurasjon via miljøvariabler.** Samme containerbilde brukes lokalt og i
  Kubernetes. Spring leser for eksempel `SPRING_DATASOURCE_URL` fra miljøet, og
  ConfigMaps og Secrets i Kubernetes setter verdiene.

## Kjente begrensninger og videre arbeid

- Hendelsen kan gå tapt hvis Kafka er nede akkurat når den skal publiseres. Løsningen er
  *transactional outbox*, som ikke er implementert.
- Ingen dead letter-topic eller kontrollert retry-strategi for meldinger som feiler.
- Kafka og Postgres kjører som enkle enkeltinstanser i klyngen, uten replikering eller backup.
- Passordet i eksemplene er et lokalt testpassord. Reelle hemmeligheter bør komme fra en
  secret manager.
- Ingen autentisering og ingen Ingress. Tjenestene nås med `port-forward`.
- Kafka-topicet `soknad.mottatt` har én partisjon, så behandlingen skalerer ikke
  horisontalt før partisjonstallet økes.