package no.flotvik.behandling_service.behandling

import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component
import tools.jackson.databind.json.JsonMapper

@Component
class SoknadMottattListener(
    private val service: BehandlingService,
    private val regelverkClient: RegelverkClient,
    private val jsonMapper: JsonMapper,
) {
    @KafkaListener(topics = ["soknad.mottatt"])
    fun mottatt(melding: String) {
        val event = jsonMapper.readValue(melding, SoknadMottatt::class.java)
        val behandling = service.start(event)
        if (behandling.status == BehandlingStatus.FERDIG) return

        val info = regelverkClient.hent(event.type)
        if (info.automatiskBehandling) {
            service.ferdigstill(event.soknadId)
        }
    }
}