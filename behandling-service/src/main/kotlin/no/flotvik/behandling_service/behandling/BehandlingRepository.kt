package no.flotvik.behandling_service.behandling

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface BehandlingRepository : JpaRepository<Behandling, UUID>