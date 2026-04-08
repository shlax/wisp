plugins {
    scala
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.scala-lang:scala3-library_3:3.8.3")

    testImplementation("org.junit.jupiter:junit-jupiter:6.0.3")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    testImplementation("com.h2database:h2:2.4.240")
}

java{
    sourceCompatibility = JavaVersion.VERSION_25
    targetCompatibility = JavaVersion.VERSION_25
}

tasks.withType<ScalaCompile>{
    scalaCompileOptions.additionalParameters = listOf("-Xunchecked-java-output-version:25")
    options.compilerArgs = listOf("-parameters")
}

tasks.withType<JavaCompile>{
    options.compilerArgs = listOf("-parameters")
}

tasks.test{
    useJUnitPlatform()
}
