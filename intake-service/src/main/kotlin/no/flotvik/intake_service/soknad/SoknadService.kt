package no.flotvik.intake_service.soknad

import io.micrometer.core.instrument.MeterRegistry
import org.springframework.context.ApplicationEventPublisher
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.time.Instant
import java.util.UUID

@Service
class SoknadService(
    private val repository: SoknadRepository,
    private val eventPublisher: ApplicationEventPublisher,
    private val meterRegistry: MeterRegistry,
) {

    @Transactional
    fun opprett(request: OpprettSoknadRequest): Soknad {
        val soknad = Soknad(
            id = UUID.randomUUID(),
            sokerNavn = request.sokerNavn,
            sokerEpost = request.sokerEpost,
            type = request.type,
            status = SoknadStatus.MOTTATT,
            opprettet = Instant.now(),
        )
        val lagret = repository.save(soknad)
        eventPublisher.publishEvent(SoknadMottatt(lagret.id, lagret.type, lagret.opprettet))
        meterRegistry.counter("soknader.mottatt").increment()
        return lagret
    }

    @Transactional
    fun markerFerdig(id: UUID) {
        val soknad = hent(id)
        soknad.status = SoknadStatus.FERDIG
        meterRegistry.counter("soknader.ferdig").increment()
    }

    @Transactional(readOnly = true)
    fun hent(id: UUID): Soknad =
        repository.findById(id).orElseThrow {
            ResponseStatusException(HttpStatus.NOT_FOUND, "Fant ikke søknad $id")
        }

    @Transactional(readOnly = true)
    fun siste(): List<Soknad> = repository.findTop20ByOrderByOpprettetDesc()
}