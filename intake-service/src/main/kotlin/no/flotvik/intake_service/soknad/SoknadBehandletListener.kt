package no.flotvik.intake_service.soknad

import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component
import tools.jackson.databind.json.JsonMapper

@Component
class SoknadBehandletListener(
    private val service: SoknadService,
    private val jsonMapper: JsonMapper,
) {
    @KafkaListener(topics = ["soknad.behandlet"])
    fun behandlet(melding: String) {
        val event = jsonMapper.readValue(melding, SoknadBehandlet::class.java)
        service.markerFerdig(event.soknadId)
    }
}