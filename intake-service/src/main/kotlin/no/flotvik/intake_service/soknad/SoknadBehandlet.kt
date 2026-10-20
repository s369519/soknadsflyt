package no.flotvik.intake_service.soknad

import java.time.Instant
import java.util.UUID

data class SoknadBehandlet(val soknadId: UUID, val behandlet: Instant)