package no.flotvik.behandling_service.kafka

import no.flotvik.behandling_service.behandling.UgyldigMeldingException
import org.apache.kafka.clients.admin.NewTopic
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.config.TopicBuilder
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer
import org.springframework.kafka.listener.DefaultErrorHandler
import org.springframework.util.backoff.ExponentialBackOff
import org.springframework.web.client.HttpClientErrorException

const val SOKNAD_BEHANDLET_TOPIC = "soknad.behandlet"

@Configuration
class KafkaConfig {

    @Bean
    fun soknadBehandletTopic(): NewTopic =
        TopicBuilder.name(SOKNAD_BEHANDLET_TOPIC).partitions(1).replicas(1).build()

    @Bean
    fun soknadMottattTopic(): NewTopic =
        TopicBuilder.name("soknad.mottatt").partitions(1).replicas(1).build()

    // Dead letter-topicet må ha minst like mange partisjoner som originaltopicet.
    @Bean
    fun soknadMottattDeadLetterTopic(): NewTopic =
        TopicBuilder.name("soknad.mottatt.DLT").partitions(1).replicas(1).build()

    @Bean
    fun kafkaErrorHandler(kafkaTemplate: KafkaTemplate<String, String>): DefaultErrorHandler {
        // Etter at forsøkene er brukt opp, publiseres meldingen til <topic>.DLT
        val recoverer = DeadLetterPublishingRecoverer(kafkaTemplate)

        // Ventetid: 0,5 s, 1 s, 2 s, 4 s
        val backOff = ExponentialBackOff(500, 2.0).apply { setMaxAttempts(4) }

        return DefaultErrorHandler(recoverer, backOff).apply {
            // Å prøve igjen hjelper ikke når meldingen er ødelagt eller typen er ukjent (4xx)
            addNotRetryableExceptions(
                UgyldigMeldingException::class.java,
                HttpClientErrorException::class.java,
            )
        }
    }
}