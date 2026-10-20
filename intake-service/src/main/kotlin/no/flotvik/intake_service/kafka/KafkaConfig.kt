package no.flotvik.intake_service.kafka

import org.apache.kafka.clients.admin.NewTopic
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.config.TopicBuilder

const val SOKNAD_MOTTATT_TOPIC = "soknad.mottatt"

@Configuration
class KafkaConfig {
    @Bean
    fun soknadMottattTopic(): NewTopic =
        TopicBuilder.name(SOKNAD_MOTTATT_TOPIC).partitions(1).replicas(1).build()

    @Bean
    fun soknadBehandletTopic(): NewTopic =
        TopicBuilder.name("soknad.behandlet").partitions(1).replicas(1).build()
}

