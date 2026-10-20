package no.flotvik.behandling_service.behandling

import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component
import tools.jackson.databind.json.JsonMapper

@Component
class SoknadMottattListener(
    private val service: BehandlingService,
    private val jsonMapper: JsonMapper,
) {
    @KafkaListener(topics = ["soknad.mottatt"])
    fun mottatt(melding: String) {
        val event = jsonMapper.readValue(melding, SoknadMottatt::class.java)
        service.start(event)
    }
}