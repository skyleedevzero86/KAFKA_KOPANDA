package com.sleekydz86

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration
import org.springframework.boot.runApplication

@SpringBootApplication(
	scanBasePackages = ["com.sleekydz86", "com.sleekydz86.kopanda"],
	exclude = [UserDetailsServiceAutoConfiguration::class]
)
class KafkaKopandaApplication

fun main(args: Array<String>) {
	runApplication<KafkaKopandaApplication>(*args)
}