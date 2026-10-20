package no.flotvik.intake_service.soknad

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

enum class SoknadStatus { MOTTATT, UNDER_BEHANDLING, FERDIG }

@Entity
@Table(name = "soknad")
class Soknad(
    @Id
    val id: UUID,

    @Column(name = "soker_navn")
    val sokerNavn: String,

    @Column(name = "soker_epost")
    val sokerEpost: String,

    @Column(name = "type")
    val type: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    var status: SoknadStatus,

    @Column(name = "opprettet")
    val opprettet: Instant,
)