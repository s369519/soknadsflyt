package no.flotvik.intake_service.kafka

import no.flotvik.intake_service.soknad.SoknadMottatt
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
    fun publiser(event: SoknadMottatt) {
        val json = jsonMapper.writeValueAsString(event)
        kafkaTemplate.send(SOKNAD_MOTTATT_TOPIC, event.soknadId.toString(), json)
            .whenComplete { _, feil ->
                if (feil != null) {
                    log.error("Kunne ikke publisere SoknadMottatt for {}", event.soknadId, feil)
                } else {
                    log.info("Publiserte SoknadMottatt for {}", event.soknadId)
                }
            }
    }
}