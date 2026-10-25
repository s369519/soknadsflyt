package no.flotvik.intake_service.soknad

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface SoknadRepository : JpaRepository<Soknad, UUID> {
    fun findTop20ByOrderByOpprettetDesc(): List<Soknad>
}