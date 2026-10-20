package no.flotvik.intake_service

import org.springframework.boot.fromApplication
import org.springframework.boot.with


fun main(args: Array<String>) {
	fromApplication<IntakeServiceApplication>().with(TestcontainersConfiguration::class).run(*args)
}
