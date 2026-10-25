package no.flotvik.behandling_service.behandling

import com.sun.net.httpserver.HttpServer
import no.flotvik.behandling_service.BehandlingContainers
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.serialization.StringDeserializer
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.kafka.ConfluentKafkaContainer
import java.net.InetSocketAddress
import java.time.Duration
import java.util.UUID

@SpringBootTest
@Import(BehandlingContainers::class)
class BehandlingIntegrationTest {

    companion object {
        // Simulerer det eksterne regelverk-API-et. Typen UKJENT gir 404.
        private val regelverkApi: HttpServer = HttpServer.create(InetSocketAddress(0), 0).apply {
            createContext("/api/soknadstyper/") { exchange ->
                val ukjent = exchange.requestURI.path.endsWith("/UKJENT")
                val body = if (ukjent) {
                    """{"feil":"ukjent type"}"""
                } else {
                    """{"type":"DAGPENGER","automatiskBehandling":true}"""
                }.toByteArray()
                exchange.responseHeaders.add("Content-Type", "application/json")
                exchange.sendResponseHeaders(if (ukjent) 404 else 200, body.size.toLong())
                exchange.responseBody.use { it.write(body) }
            }
            start()
        }

        @JvmStatic
        @DynamicPropertySource
        fun properties(registry: DynamicPropertyRegistry) {
            registry.add("regelverk.url") { "http://localhost:${regelverkApi.address.port}" }
        }
    }

    @Autowired
    lateinit var kafka: ConfluentKafkaContainer

    @Autowired
    lateinit var kafkaTemplate: KafkaTemplate<String, String>

    @Autowired
    lateinit var repository: BehandlingRepository

    private fun soknadMottatt(id: UUID, type: String = "DAGPENGER") =
        """{"soknadId":"$id","type":"$type","mottatt":"2026-10-20T12:00:00Z"}"""

    private fun konsument(topic: String): KafkaConsumer<String, String> {
        val props = mapOf<String, Any>(
            ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG to kafka.bootstrapServers,
            ConsumerConfig.GROUP_ID_CONFIG to "test-${UUID.randomUUID()}",
            ConsumerConfig.AUTO_OFFSET_RESET_CONFIG to "earliest",
            ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java.name,
            ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java.name,
        )
        return KafkaConsumer<String, String>(props).also { it.subscribe(listOf(topic)) }
    }

    private fun ventPaaMelding(consumer: KafkaConsumer<String, String>, noekkel: String): String {
        var verdi: String? = null
        await().atMost(Duration.ofSeconds(30)).untilAsserted {
            consumer.poll(Duration.ofMillis(500)).forEach { if (it.key() == noekkel) verdi = it.value() }
            assertNotNull(verdi, "Fant ingen melding med nøkkel $noekkel")
        }
        return verdi!!
    }

    @Test
    fun `behandler soknad og publiserer SoknadBehandlet`() {
        val id = UUID.randomUUID()

        konsument("soknad.behandlet").use { consumer ->
            kafkaTemplate.send("soknad.mottatt", id.toString(), soknadMottatt(id))

            await().atMost(Duration.ofSeconds(30)).untilAsserted {
                val status = repository.findById(id).orElse(null)?.status
                assertEquals(BehandlingStatus.FERDIG, status)
            }

            val nokler = mutableListOf<String>()
            await().atMost(Duration.ofSeconds(20)).untilAsserted {
                consumer.poll(Duration.ofMillis(500)).forEach { nokler.add(it.key()) }
                assertEquals(1, nokler.count { it == id.toString() })
            }
        }
    }

    @Test
    fun `samme melding to ganger gir kun en behandling og en hendelse`() {
        val id = UUID.randomUUID()

        konsument("soknad.behandlet").use { consumer ->
            kafkaTemplate.send("soknad.mottatt", id.toString(), soknadMottatt(id))
            kafkaTemplate.send("soknad.mottatt", id.toString(), soknadMottatt(id))

            await().atMost(Duration.ofSeconds(30)).untilAsserted {
                assertEquals(BehandlingStatus.FERDIG, repository.findById(id).orElse(null)?.status)
            }

            // Hold utkikk en stund for å fange en eventuell dobbel hendelse
            val nokler = mutableListOf<String>()
            val slutt = System.currentTimeMillis() + 5_000
            while (System.currentTimeMillis() < slutt) {
                consumer.poll(Duration.ofMillis(500)).forEach { nokler.add(it.key()) }
            }
            assertEquals(1, nokler.count { it == id.toString() })
        }
    }

    @Test
    fun `ugyldig melding havner paa dead letter-topic`() {
        val noekkel = "ugyldig-${UUID.randomUUID()}"

        konsument("soknad.mottatt.DLT").use { consumer ->
            kafkaTemplate.send("soknad.mottatt", noekkel, "dette er ikke json")

            assertEquals("dette er ikke json", ventPaaMelding(consumer, noekkel))
        }
    }

    @Test
    fun `ukjent soknadstype gir 404 og havner paa dead letter-topic`() {
        val id = UUID.randomUUID()

        konsument("soknad.mottatt.DLT").use { consumer ->
            kafkaTemplate.send("soknad.mottatt", id.toString(), soknadMottatt(id, "UKJENT"))

            ventPaaMelding(consumer, id.toString())

            // Behandlingen ble startet, men aldri ferdigstilt
            assertEquals(BehandlingStatus.UNDER_BEHANDLING, repository.findById(id).orElse(null)?.status)
        }
    }
}