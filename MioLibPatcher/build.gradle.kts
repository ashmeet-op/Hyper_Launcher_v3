plugins {
    java
}

group = "com.mio"


dependencies {
    implementation("org.javassist:javassist:3.30.2-GA")
    //noinspection NewerVersionAvailable
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

tasks.test {
    useJUnitPlatform()
    jvmArgs("--add-opens=java.base/java.lang=ALL-UNNAMED")
}

tasks.jar {
    archiveFileName.set("MioLibPatcher.jar")
    manifest {
        attributes(
            "Manifest-Version" to "1.0",
            "Premain-Class" to "com.mio.libpatcher.MainAgent",
            "Agent-Class" to "com.mio.libpatcher.MainAgent",
            "Can-Redefine-Classes" to true,
            "Can-Retransform-Classes" to true
        )
    }
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
    exclude("META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA")
}
