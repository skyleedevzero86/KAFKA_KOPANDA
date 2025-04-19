plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}
rootProject.name = "backend"
include("kpanda-application")
include("kpanda-domain")
include("kpanda-infrastructure")
include("kpanda-interface")
