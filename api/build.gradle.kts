plugins {

}


dependencies {
    compileOnly("org.slf4j:slf4j-api:2.0.9")
    compileOnly("org.jetbrains:annotations:26.0.2")
    compileOnly("com.google.code.gson:gson:2.13.2")
    compileOnly("redis.clients:jedis:7.2.0")


    testImplementation("com.google.code.gson:gson:2.13.2")
    testImplementation("org.slf4j:slf4j-api:2.0.9")
    testImplementation("org.slf4j:slf4j-simple:2.0.17")
    testImplementation("redis.clients:jedis:7.2.0")

    testImplementation(platform("org.junit:junit-bom:5.10.2"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("org.assertj:assertj-core:3.25.3")
    testImplementation("org.mockito:mockito-core:5.11.0")
    testImplementation("org.mockito:mockito-junit-jupiter:5.11.0")
    testImplementation("org.awaitility:awaitility:4.3.0")

    testImplementation("org.testcontainers:testcontainers:2.0.3")
    testImplementation("org.testcontainers:junit-jupiter:1.21.4") // JUnit 5 Integration
    testImplementation("org.testcontainers:postgresql:1.21.4")
    testImplementation("com.zaxxer:HikariCP:5.1.0")
    testImplementation("org.postgresql:postgresql:42.7.3")
    testImplementation("redis.clients:jedis:6.0.0")
}

tasks {
    test {
        useJUnitPlatform()
    }
}