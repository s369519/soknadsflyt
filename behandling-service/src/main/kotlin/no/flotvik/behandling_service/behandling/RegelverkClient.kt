package no.flotvik.behandling_service.behandling

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient

data class SoknadstypeInfo(val type: String, val automatiskBehandling: Boolean)

@Component
class RegelverkClient(@Value("\${regelverk.url}") baseUrl: String) {
    private val client = RestClient.create(baseUrl)

    fun hent(type: String): SoknadstypeInfo =
        client.get()
            .uri("/api/soknadstyper/{type}", type)
            .retrieve()
            .body(SoknadstypeInfo::class.java)
            ?: error("Tomt svar fra regelverk-api for type $type")
}