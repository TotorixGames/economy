import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("java-library")
    id("maven-publish")
    id("com.gradleup.shadow") version "9.0.0"


}

allprojects {
    group = "it.einjojo"
    version = "2.1.0-SNAPSHOT"
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
        relocate("io.lettuce", "net.wandoria.economyprovider.libs.lettuce")
        relocate("io.netty", "net.wandoria.economyprovider.libs.netty")
        relocate("reactor", "net.wandoria.economyprovider.libs.reactor")
        relocate("redis.clients.authentication.core", "net.wandoria.economyprovider.libs.redisauthcore")
        relocate("org.reactivestreams", "net.wandoria.economyprovider.libs.reactivestreams")
    }


}
