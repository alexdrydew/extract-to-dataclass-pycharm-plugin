plugins {
    id("java")
    id("org.jetbrains.intellij") version "1.17.3"
}

group = project.property("group") as String
version = project.property("version") as String

repositories {
    mavenCentral()
}

intellij {
    version.set(project.property("intellijVersion") as String)
    type.set(project.property("intellijType") as String)
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
    testImplementation("junit:junit:4.13.1")
}

tasks {
    withType<JavaCompile> {
        sourceCompatibility = project.property("javaVersion") as String
        targetCompatibility = project.property("javaVersion") as String
    }

    patchPluginXml {
        sinceBuild.set(project.property("pluginSinceBuild") as String)
        untilBuild.set(project.property("pluginUntilBuild") as String)
    }

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
