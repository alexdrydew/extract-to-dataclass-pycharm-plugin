plugins {
    id("java")
    id("org.jetbrains.intellij") version "1.17.3"
}

group = "com.extractToDataclass"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

intellij {
    version.set("2024.1.3")
    type.set("PC")
    plugins.set(listOf("PythonCore"))
}

sourceSets {
    main {}
    test {
        java.srcDirs("testSrc")
        resources.srcDirs("testData")
    }
}

dependencies {
    testImplementation("junit:junit:4.13")
}

tasks {
    withType<JavaCompile> {
        sourceCompatibility = "17"
        targetCompatibility = "17"
    }

//    patchPluginXml {
//        sinceBuild.set("232")
//        untilBuild.set("242.*")
//    }

    signPlugin {
        certificateChain.set(System.getenv("CERTIFICATE_CHAIN"))
        privateKey.set(System.getenv("PRIVATE_KEY"))
        password.set(System.getenv("PRIVATE_KEY_PASSWORD"))
    }

    publishPlugin {
        token.set(System.getenv("PUBLISH_TOKEN"))
    }

    test {
        useJUnit()
    }
}
