package no.flotvik.behandling_service.behandling

import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.time.Instant
import java.util.UUID

@Service
class BehandlingService(private val repository: BehandlingRepository) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun start(event: SoknadMottatt) {
        // Kafka leverer minst én gang, så samme melding kan komme flere ganger.
        if (repository.existsById(event.soknadId)) {
            log.info("Søknad {} er allerede registrert, hopper over", event.soknadId)
            return
        }
        repository.save(Behandling(event.soknadId, BehandlingStatus.UNDER_BEHANDLING, Instant.now()))
        log.info("Startet behandling av søknad {}", event.soknadId)
    }

    @Transactional(readOnly = true)
    fun hent(soknadId: UUID): Behandling =
        repository.findById(soknadId).orElseThrow {
            ResponseStatusException(HttpStatus.NOT_FOUND, "Fant ingen behandling for søknad $soknadId")
        }
}