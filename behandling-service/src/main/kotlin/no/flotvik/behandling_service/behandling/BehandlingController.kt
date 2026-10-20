package no.flotvik.behandling_service.behandling

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.Instant
import java.util.UUID

data class BehandlingResponse(
    val soknadId: UUID,
    val status: BehandlingStatus,
    val oppdatert: Instant,
)

@RestController
@RequestMapping("/behandlinger")
class BehandlingController(private val service: BehandlingService) {

    @GetMapping("/{soknadId}")
    fun hent(@PathVariable soknadId: UUID): BehandlingResponse {
        val b = service.hent(soknadId)
        return BehandlingResponse(b.soknadId, b.status, b.oppdatert)
    }
}