plugins {
    kotlin("jvm")
    application
}

dependencies {
    implementation(project(":core"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
}

application {
    mainClass.set("net.tenbo.hubble.demo.DemoKt")
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

// Test helper: deposit a real decryptable chat into a phone's mailbox to verify notifications.
// ./gradlew :demo:inject -PmailboxId=<id> [-PbaseUrl=...]
tasks.register<JavaExec>("inject") {
    group = "verification"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("net.tenbo.hubble.demo.InjectKt")
    args = listOfNotNull(
        (project.findProperty("mailboxId") as String?)?.let { "mailboxId=$it" },
        (project.findProperty("baseUrl") as String?)?.let { "baseUrl=$it" },
    )
}
