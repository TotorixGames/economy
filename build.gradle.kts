import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("java-library")
    id("maven-publish")
    id("com.gradleup.shadow") version "9.0.0"
    id("de.eldoria.plugin-yml.paper") version "0.8.0"

}
group = "it.einjojo"
version = "0.0.1-SNAPSHOT"
allprojects {
    repositories {

    }
}
subprojects {
    apply(plugin = "java-library")
    apply(plugin = "com.gradleup.shadow")
    apply(plugin = "maven-publish")

    repositories {
        mavenCentral()
        maven {
            name = "papermc"
            url = uri("https://repo.papermc.io/repository/maven-public/")
        }
        maven {
            name = "jitpack"
            url = uri("https://jitpack.io")
        }
    }
    java {
        toolchain.languageVersion.set(JavaLanguageVersion.of(25))
    }

    tasks.named("shadowJar", ShadowJar::class) {
        mergeServiceFiles()
        archiveFileName.set("${rootProject.name}-${project.version}-${archiveFileName.get()}.jar")
        relocate("io.lettuce", "net.wandoria.economyprovider.libs.lettuce")
        relocate("io.netty", "net.wandoria.economyprovider.libs.netty")
        relocate("reactor", "net.wandoria.economyprovider.libs.reactor")
        relocate("redis.clients.authentication.core", "net.wandoria.economyprovider.libs.redisauthcore")
        relocate("org.reactivestreams", "net.wandoria.economyprovider.libs.reactivestreams")
    }


}
