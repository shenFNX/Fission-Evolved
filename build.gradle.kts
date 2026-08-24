
import org.gradle.jvm.tasks.Jar

plugins {
    id("com.gtnewhorizons.gtnhconvention")
}

val releaseVersion = providers.gradleProperty("modVersion").get()
version = releaseVersion
extensions.extraProperties["modVersion"] = releaseVersion

tasks.withType<Jar>().configureEach {
    manifest {
        attributes(
            "Specification-Title" to providers.gradleProperty("modName").get(),
            "Specification-Version" to releaseVersion,
            "Specification-Vendor" to "Shen FNX",
            "Implementation-Title" to providers.gradleProperty("modName").get(),
            "Implementation-Version" to releaseVersion,
            "Implementation-Vendor" to "Shen FNX",
        )
    }
}
