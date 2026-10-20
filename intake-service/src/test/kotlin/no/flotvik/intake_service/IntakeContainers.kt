package no.flotvik.intake_service

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.context.annotation.Bean
import org.testcontainers.kafka.ConfluentKafkaContainer
import org.testcontainers.postgresql.PostgreSQLContainer

@TestConfiguration(proxyBeanMethods = false)
class IntakeContainers {

    @Bean
    @ServiceConnection
    fun postgres(): PostgreSQLContainer = PostgreSQLContainer("postgres:16")

    @Bean
    @ServiceConnection
    fun kafka(): ConfluentKafkaContainer = ConfluentKafkaContainer("confluentinc/cp-kafka:7.7.0")
}