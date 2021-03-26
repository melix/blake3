import me.champeau.jmh.WithJavaToolchain

plugins {
    java
    `maven-publish`
    id("me.champeau.jmh") version "0.6.3"
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation("org.json:json:20190722")
    testImplementation("junit:junit:4.13.1")
}

group = "io.github.rctcwyvrn"
version = "1.4-SNAPSHOT"
description = "blake3"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(8))
    }
    withSourcesJar()
    withJavadocJar()
}

val java16 by sourceSets.creating {
    java.srcDir("src/main/java16")
}

dependencies {
    "java16Implementation"(objects.fileCollection().from(sourceSets.main.get().java.classesDirectory))
    jmhImplementation(java16.output.classesDirs)
    jmhImplementation("io.lktk:blake3jni:0.2.2")
}

val addVectorModule = "--add-modules=jdk.incubator.vector"

val java16Classes = tasks.named<JavaCompile>("compileJava16Java") {
    javaCompiler.set(javaToolchains.compilerFor {
        languageVersion.set(JavaLanguageVersion.of(16))
    })
    options.compilerArgs.add("--add-modules=jdk.incubator.vector")
}

val java16Test = tasks.register<Test>("java16Test") {
    val testClasspath = objects.fileCollection().from(tasks.jar)
    testClasspath.from(sourceSets.test.get().runtimeClasspath)
    classpath = testClasspath
    testClassesDirs = sourceSets.test.get().output.classesDirs
    javaLauncher.set(javaToolchains.launcherFor {
        languageVersion.set(JavaLanguageVersion.of(16))
    })
    jvmArgs = listOf(addVectorModule)
}

tasks.withType<JavaExec> {
    jvmArgs = listOf(addVectorModule)
}

tasks.jar {
    into("META-INF/versions/16") {
        from(java16.output)
    }
    manifest.attributes(
            "Multi-Release" to "true"
    )
}

publishing {
    publications.create<MavenPublication>("maven") {
        from(components["java"])
    }
}

tasks.matching { it is WithJavaToolchain }.configureEach {
    this as WithJavaToolchain
    javaLauncher.set(javaToolchains.launcherFor {
        languageVersion.set(JavaLanguageVersion.of(16))
    })
}

tasks.matching { it.name in listOf("compileJmhJava", "jmhCompileGeneratedClasses") }.configureEach {
    this as JavaCompile
    javaCompiler.set(javaToolchains.compilerFor {
        languageVersion.set(JavaLanguageVersion.of(16))
    })
}

jmh {
    includeTests.set(false)
    warmupIterations.set(4)
    fork.set(3)
    iterations.set(5)
    warmup.set("1s")
    timeOnIteration.set("1s")
    operationsPerInvocation.set(10)
    jvmArgs.set(listOf(addVectorModule))
}
