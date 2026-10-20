package no.flotvik.behandling_service.behandling

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

enum class BehandlingStatus { UNDER_BEHANDLING, FERDIG }

@Entity
@Table(name = "behandling")
class Behandling(
    @Id
    @Column(name = "soknad_id")
    val soknadId: UUID,

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    var status: BehandlingStatus,

    @Column(name = "oppdatert")
    var oppdatert: Instant,
)