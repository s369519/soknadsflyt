package no.flotvik.intake_service

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class IntakeServiceApplication

fun main(args: Array<String>) {
	runApplication<IntakeServiceApplication>(*args)
}
