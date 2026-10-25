package no.flotvik.behandling_service.behandling

import io.micrometer.core.instrument.MeterRegistry
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component
import tools.jackson.databind.json.JsonMapper
import java.util.concurrent.TimeUnit

@Component
class SoknadMottattListener(
    private val service: BehandlingService,
    private val regelverkClient: RegelverkClient,
    private val jsonMapper: JsonMapper,
    private val meterRegistry: MeterRegistry,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @KafkaListener(topics = ["soknad.mottatt"])
    fun mottatt(melding: String) {
        val event = try {
            jsonMapper.readValue(melding, SoknadMottatt::class.java)
        } catch (e: Exception) {
            meterRegistry.counter("behandling.forsok.feilet").increment()
            throw UgyldigMeldingException("Kunne ikke lese meldingen som SoknadMottatt", e)
        }

        try {
            val behandling = service.start(event)
            if (behandling.status == BehandlingStatus.FERDIG) return

            val info = maalRegelverkKall { regelverkClient.hent(event.type) }
            if (info.automatiskBehandling) {
                service.ferdigstill(event.soknadId)
                meterRegistry.counter("behandlinger.ferdigstilt").increment()
            }
        } catch (e: Exception) {
            meterRegistry.counter("behandling.forsok.feilet").increment()
            log.warn("Behandling av søknad {} feilet: {}", event.soknadId, e.message)
            throw e
        }
    }

    // Måler tiden på kallet, også når det feiler.
    private fun <T> maalRegelverkKall(kall: () -> T): T {
        val start = System.nanoTime()
        try {
            return kall()
        } finally {
            meterRegistry.timer("regelverk.kall").record(System.nanoTime() - start, TimeUnit.NANOSECONDS)
        }
    }
}