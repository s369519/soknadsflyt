package no.flotvik.behandling_service

import org.springframework.boot.fromApplication
import org.springframework.boot.with


fun main(args: Array<String>) {
	fromApplication<BehandlingServiceApplication>().with(TestcontainersConfiguration::class).run(*args)
}
