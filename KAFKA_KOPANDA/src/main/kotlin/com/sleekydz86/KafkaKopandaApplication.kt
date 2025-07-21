package com.sleekydz86

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class KafkaKopandaApplication

fun main(args: Array<String>) {
	runApplication<KafkaKopandaApplication>(*args)
}
