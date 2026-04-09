plugins {
    scala
    `maven-publish`
    `java-library`
}

repositories {
    mavenCentral()
}

group = "org.wisp"
version = "0.0.1-SNAPSHOT"

dependencies {
    implementation("org.scala-lang:scala3-library_3:3.8.3")

    testImplementation("org.junit.jupiter:junit-jupiter:6.0.3")
    testImplementation("com.h2database:h2:2.4.240")

    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

java{
    sourceCompatibility = JavaVersion.VERSION_25
    targetCompatibility = JavaVersion.VERSION_25

    withSourcesJar()
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

publishing{

    publications{
        create<MavenPublication>("maven") {
            from(components["java"])
        }
    }

    repositories{
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/shlax/wisp")
            credentials {
                username = System.getenv("GITHUB_ACTOR")
                password = System.getenv("GITHUB_TOKEN")
            }
        }
    }
}
