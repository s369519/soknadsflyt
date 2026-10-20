package no.flotvik.intake_service.soknad

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import java.time.Instant
import java.util.UUID

data class OpprettSoknadRequest(
    @field:NotBlank val sokerNavn: String,
    @field:NotBlank @field:Email val sokerEpost: String,
    @field:NotBlank val type: String,
)

data class SoknadResponse(
    val id: UUID,
    val sokerNavn: String,
    val sokerEpost: String,
    val type: String,
    val status: SoknadStatus,
    val opprettet: Instant,
)

fun Soknad.tilResponse() = SoknadResponse(id, sokerNavn, sokerEpost, type, status, opprettet)