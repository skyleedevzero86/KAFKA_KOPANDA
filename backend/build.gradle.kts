import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
	kotlin("jvm") version "1.9.25" apply false
	kotlin("plugin.spring") version "1.9.25" apply false
	kotlin("plugin.jpa") version "1.9.25"
	id("org.springframework.boot") version "3.4.4" apply false
	id("io.spring.dependency-management") version "1.1.7" apply false
}

allprojects {
	group = "com"
	version = "0.0.1-SNAPSHOT"

	tasks.withType<JavaCompile> {
		sourceCompatibility = "19"
		targetCompatibility = "19"
	}

	tasks.withType<Test> {
		useJUnitPlatform()
	}

	tasks.withType<KotlinCompile> {
		kotlinOptions {
			freeCompilerArgs = listOf("-Xjsr305=strict")
			jvmTarget = "19"
		}
	}

	repositories {
		mavenCentral()
	}
}

subprojects {
	apply(plugin = "org.jetbrains.kotlin.jvm")
	apply(plugin = "org.jetbrains.kotlin.plugin.spring")
	apply(plugin = "org.jetbrains.kotlin.plugin.jpa")
	apply(plugin = "org.springframework.boot")
	apply(plugin = "io.spring.dependency-management")

	// 서브 모듈 공통 라이브러리
	// 현업 학습을 위한 MSA 환경을 만들기 위함으로 Spring, Jpa 등등은 공통 선언
	dependencies {
		val kotestVersion = "5.9.1"

		// Spring, JPA, kotlin
		add("implementation", "org.springframework.boot:spring-boot-starter-web")
		add("implementation", "org.springframework.boot:spring-boot-starter")
		add("testImplementation", "org.springframework.boot:spring-boot-starter-test")
		add("implementation", "org.springframework.boot:spring-boot-starter-validation")
		add("implementation", "org.springframework.boot:spring-boot-starter-data-jpa")
		add("implementation", "com.fasterxml.jackson.module:jackson-module-kotlin")
		add("implementation", "org.jetbrains.kotlin:kotlin-reflect")
		add("testImplementation", "org.jetbrains.kotlin:kotlin-test-junit5")
		add("testRuntimeOnly", "org.junit.platform:junit-platform-launcher")

		// kotest
		add("testImplementation", "io.kotest:kotest-runner-junit5:$kotestVersion")
		add("testImplementation", "io.kotest:kotest-assertions-core:$kotestVersion")
		add("testImplementation", "io.kotest:kotest-property:$kotestVersion")

		// mockk
		add("testImplementation", "io.mockk:mockk:1.13.11")
		add("testImplementation", "com.ninja-squad:springmockk:4.0.2")

		// RestAssured
		add("testImplementation", "io.rest-assured:rest-assured:5.5.0")

		// h2
		add("runtimeOnly", "com.h2database:h2")

		// Swagger
		add("implementation", "org.springdoc:springdoc-openapi-starter-webmvc-ui:2.6.0")

		add("implementation", "org.apache.kafka:kafka-clients:3.9.0")
		add("implementation", "com.fasterxml.jackson.core:jackson-databind:2.18.1")
		add("implementation", "io.confluent:kafka-avro-serializer:7.7.1")
		add("implementation", "com.google.protobuf:protobuf-java:4.28.3")
	}
}
