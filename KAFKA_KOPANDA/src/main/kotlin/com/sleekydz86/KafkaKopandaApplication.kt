package com.sleekydz86

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication(scanBasePackages = ["com.sleekydz86.kopanda"])
class KafkaKopandaApplication

fun main(args: Array<String>) {
	runApplication<KafkaKopandaApplication>(*args)
}