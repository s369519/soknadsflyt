package no.flotvik.behandling_service.kafka

import no.flotvik.behandling_service.behandling.SoknadBehandlet
import org.slf4j.LoggerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener
import tools.jackson.databind.json.JsonMapper

@Component
class SoknadEventPublisher(
    private val kafkaTemplate: KafkaTemplate<String, String>,
    private val jsonMapper: JsonMapper,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun publiser(event: SoknadBehandlet) {
        val json = jsonMapper.writeValueAsString(event)
        kafkaTemplate.send(SOKNAD_BEHANDLET_TOPIC, event.soknadId.toString(), json)
            .whenComplete { _, feil ->
                if (feil != null) log.error("Kunne ikke publisere SoknadBehandlet for {}", event.soknadId, feil)
                else log.info("Publiserte SoknadBehandlet for {}", event.soknadId)
            }
    }
}