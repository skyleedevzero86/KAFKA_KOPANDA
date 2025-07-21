plugins {
	kotlin("jvm") version "1.9.25"
	kotlin("plugin.spring") version "1.9.25"
	id("org.springframework.boot") version "3.5.3"
	id("io.spring.dependency-management") version "1.1.7"
}

group = "com"
version = "0.0.1-SNAPSHOT"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(19)
	}
}

repositories {
	mavenCentral()
	maven { url = uri("https://packages.confluent.io/maven/") }
}

dependencies {
	implementation("org.springframework.boot:spring-boot-starter-web")
	implementation("org.apache.kafka:kafka-clients:3.9.0")
	implementation("com.fasterxml.jackson.core:jackson-databind:2.18.1")
	implementation("io.confluent:kafka-avro-serializer:7.7.1")
	implementation("com.google.protobuf:protobuf-java:4.28.3")
	testImplementation("org.springframework.boot:spring-boot-starter-test")
}

kotlin {
	compilerOptions {
		freeCompilerArgs.addAll("-Xjsr305=strict")
	}
}

tasks.withType<Test> {
	useJUnitPlatform()
}
