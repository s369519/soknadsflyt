package no.flotvik.intake_service.soknad

import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.net.URI
import java.util.UUID

@RestController
@RequestMapping("/soknader")
class SoknadController(private val service: SoknadService) {

    @PostMapping
    fun opprett(@Valid @RequestBody request: OpprettSoknadRequest): ResponseEntity<SoknadResponse> {
        val soknad = service.opprett(request)
        return ResponseEntity.created(URI.create("/soknader/${soknad.id}")).body(soknad.tilResponse())
    }

    @GetMapping
    fun liste(): List<SoknadResponse> = service.siste().map { it.tilResponse() }

    @GetMapping("/{id}")
    fun hent(@PathVariable id: UUID): SoknadResponse = service.hent(id).tilResponse()
}