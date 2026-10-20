package no.flotvik.behandling_service.behandling

import java.time.Instant
import java.util.UUID

data class SoknadBehandlet(val soknadId: UUID, val behandlet: Instant)