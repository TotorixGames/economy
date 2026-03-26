import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("java-library")
    id("maven-publish")
    id("com.gradleup.shadow") version "9.0.0"
}

allprojects {
    group = "it.einjojo"
    version = "2.1.1-SNAPSHOT"
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
        toolchain.languageVersion.set(JavaLanguageVersion.of(21))
    }

    tasks.assemble {
        dependsOn("shadowJar")
    }
    tasks.compileJava {
        options.encoding = "UTF-8"
    }


    tasks.named("shadowJar", ShadowJar::class) {
        mergeServiceFiles()
        archiveFileName.set("${rootProject.name}-${archiveFileName.get()}")
        relocate("io.lettuce", "it.einjojo.economy.libs.lettuce")
        relocate("io.netty", "it.einjojo.economy.libs.netty")
        relocate("reactor", "it.einjojo.economy.libs.reactor")
        relocate("redis.clients.authentication.core", "it.einjojo.economy.libs.redisauthcore")
        relocate("org.reactivestreams", "it.einjojo.economy.libs.reactivestreams")
    }


}
