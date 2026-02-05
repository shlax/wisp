plugins {
    scala
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.scala-lang:scala3-library_3:3.8.1")

    testImplementation("org.junit.jupiter:junit-jupiter:6.0.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    testImplementation("com.h2database:h2:2.4.240")
}

tasks.test{
    useJUnitPlatform()
}
