import net.minecrell.pluginyml.paper.PaperPluginDescription

plugins {
    id("de.eldoria.plugin-yml.paper") version "0.8.0"
}
dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.8-R0.1-SNAPSHOT")
    compileOnly("com.github.MilkBowl:VaultAPI:1.7.1")
    paperLibrary("com.github.ben-manes.caffeine:caffeine:3.1.8")
    paperLibrary("com.zaxxer:HikariCP:7.0.2")
    paperLibrary("org.postgresql:postgresql:42.7.8")
    implementation("io.lettuce:lettuce-core:6.8.1.RELEASE")
    api(project(":api"))
    compileOnly(fileTree("../libs"))
}


paper {
    name = "EconomyProviderPlugin"
    main = "it.einjojo.economy.EconomyPlugin"
    foliaSupported = true
    authors = listOf("EinJOJO")
    description = "economy plugin"
    website = "https://einjojo.it"
    apiVersion = "1.21"
    version = rootProject.version.toString()
    loader = "it.einjojo.economy.PluginLibrariesLoader"
    generateLibrariesJson = true
    serverDependencies {
        register("Vault") {
            required = false
            load = PaperPluginDescription.RelativeLoadOrder.BEFORE
        }
        register("TinyMarkets") {
            required = false
            load = PaperPluginDescription.RelativeLoadOrder.AFTER
        }
        register("zShop") {
            required = false
            load = PaperPluginDescription.RelativeLoadOrder.BEFORE
        }
    }
}