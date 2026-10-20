package no.flotvik.intake_service.soknad

import java.time.Instant
import java.util.UUID

data class SoknadMottatt(
    val soknadId: UUID,
    val type: String,
    val mottatt: Instant,
)