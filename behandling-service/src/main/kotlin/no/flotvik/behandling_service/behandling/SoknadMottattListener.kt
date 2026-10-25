package no.flotvik.behandling_service.behandling

import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component
import tools.jackson.databind.json.JsonMapper

@Component
class SoknadMottattListener(
    private val service: BehandlingService,
    private val regelverkClient: RegelverkClient,
    private val jsonMapper: JsonMapper,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @KafkaListener(topics = ["soknad.mottatt"])
    fun mottatt(melding: String) {
        val event = try {
            jsonMapper.readValue(melding, SoknadMottatt::class.java)
        } catch (e: Exception) {
            throw UgyldigMeldingException("Kunne ikke lese meldingen som SoknadMottatt", e)
        }

        try {
            val behandling = service.start(event)
            if (behandling.status == BehandlingStatus.FERDIG) return

            val info = regelverkClient.hent(event.type)
            if (info.automatiskBehandling) {
                service.ferdigstill(event.soknadId)
            }
        } catch (e: Exception) {
            log.warn("Behandling av søknad {} feilet: {}", event.soknadId, e.message)
            throw e
        }
    }
}