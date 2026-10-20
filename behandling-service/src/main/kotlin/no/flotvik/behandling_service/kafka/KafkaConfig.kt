package no.flotvik.behandling_service.kafka

import org.apache.kafka.clients.admin.NewTopic
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.config.TopicBuilder

const val SOKNAD_BEHANDLET_TOPIC = "soknad.behandlet"

@Configuration
class KafkaConfig {
    @Bean
    fun soknadBehandletTopic(): NewTopic =
        TopicBuilder.name(SOKNAD_BEHANDLET_TOPIC).partitions(1).replicas(1).build()
}