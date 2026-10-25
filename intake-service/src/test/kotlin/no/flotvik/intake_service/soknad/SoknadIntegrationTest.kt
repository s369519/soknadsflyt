package no.flotvik.intake_service.soknad

import no.flotvik.intake_service.IntakeContainers
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.serialization.StringDeserializer
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestClient
import org.testcontainers.kafka.ConfluentKafkaContainer
import java.time.Duration
import java.util.UUID

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = ["management.prometheus.metrics.export.enabled=true"],)
@Import(IntakeContainers::class)
class SoknadIntegrationTest {

    @Value("\${local.server.port}")
    var port: Int = 0

    @Autowired
    lateinit var kafka: ConfluentKafkaContainer

    @Autowired
    lateinit var repository: SoknadRepository

    @Autowired
    lateinit var kafkaTemplate: KafkaTemplate<String, String>

    private val gyldigSoknad =
        """{"sokerNavn":"Kari Nordmann","sokerEpost":"kari@example.com","type":"DAGPENGER"}"""

    private fun client() = RestClient.create("http://localhost:$port")

    private fun opprettSoknad(): String {
        val respons = client().post().uri("/soknader")
            .contentType(MediaType.APPLICATION_JSON)
            .body(gyldigSoknad)
            .retrieve()
            .toEntity(Map::class.java)
        assertEquals(201, respons.statusCode.value())
        return respons.body!!["id"] as String
    }

    @Test
    fun `oppretter og henter soknad`() {
        val id = opprettSoknad()

        val hentet = client().get().uri("/soknader/{id}", id)
            .retrieve()
            .body(Map::class.java)!!

        assertEquals("MOTTATT", hentet["status"])
        assertEquals("Kari Nordmann", hentet["sokerNavn"])
    }

    @Test
    fun `avviser soknad med ugyldig epost`() {
        assertThrows<HttpClientErrorException.BadRequest> {
            client().post().uri("/soknader")
                .contentType(MediaType.APPLICATION_JSON)
                .body("""{"sokerNavn":"Kari","sokerEpost":"ikke-en-epost","type":"DAGPENGER"}""")
                .retrieve()
                .toBodilessEntity()
        }
    }

    @Test
    fun `publiserer SoknadMottatt paa Kafka etter lagring`() {
        val props = mapOf<String, Any>(
            ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG to kafka.bootstrapServers,
            ConsumerConfig.GROUP_ID_CONFIG to "test-${UUID.randomUUID()}",
            ConsumerConfig.AUTO_OFFSET_RESET_CONFIG to "earliest",
            ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java.name,
            ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java.name,
        )

        KafkaConsumer<String, String>(props).use { consumer ->
            consumer.subscribe(listOf("soknad.mottatt"))
            val id = opprettSoknad()

            val nokler = mutableListOf<String>()
            await().atMost(Duration.ofSeconds(20)).untilAsserted {
                consumer.poll(Duration.ofMillis(500)).forEach { nokler.add(it.key()) }
                assertTrue(nokler.contains(id), "Fant ikke hendelse for søknad $id")
            }
        }
    }

    @Test
    fun `setter soknad til FERDIG naar SoknadBehandlet mottas`() {
        val id = opprettSoknad()

        kafkaTemplate.send(
            "soknad.behandlet",
            id,
            """{"soknadId":"$id","behandlet":"2026-10-20T12:00:00Z"}""",
        )

        await().atMost(Duration.ofSeconds(20)).untilAsserted {
            val status = repository.findById(UUID.fromString(id)).get().status
            assertEquals(SoknadStatus.FERDIG, status)
        }
    }

    @Test
    fun `lister siste soknader`() {
        val id = opprettSoknad()

        val liste = client().get().uri("/soknader")
            .retrieve()
            .body(List::class.java)!!

        assertTrue(liste.any { (it as Map<*, *>)["id"] == id })
    }

    @Test
    fun `eksponerer metrikker for mottatte soknader`() {
        opprettSoknad()

        val metrikker = client().get().uri("/actuator/prometheus")
            .retrieve()
            .body(String::class.java)!!

        assertTrue(metrikker.contains("soknader_mottatt_total"))
    }
}