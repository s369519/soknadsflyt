package no.flotvik.behandling_service.behandling

import org.springframework.context.ApplicationEventPublisher
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.time.Instant
import java.util.UUID

@Service
class BehandlingService(
    private val repository: BehandlingRepository,
    private val eventPublisher: ApplicationEventPublisher,
) {

    // Idempotent: samme melding kan leveres flere ganger.
    @Transactional
    fun start(event: SoknadMottatt): Behandling =
        repository.findById(event.soknadId).orElseGet {
            repository.save(Behandling(event.soknadId, BehandlingStatus.UNDER_BEHANDLING, Instant.now()))
        }

    @Transactional
    fun ferdigstill(soknadId: UUID) {
        val behandling = hent(soknadId)
        if (behandling.status == BehandlingStatus.FERDIG) return
        behandling.status = BehandlingStatus.FERDIG
        behandling.oppdatert = Instant.now()
        eventPublisher.publishEvent(SoknadBehandlet(soknadId, behandling.oppdatert))
    }

    @Transactional(readOnly = true)
    fun hent(soknadId: UUID): Behandling =
        repository.findById(soknadId).orElseThrow {
            ResponseStatusException(HttpStatus.NOT_FOUND, "Fant ingen behandling for søknad $soknadId")
        }
}