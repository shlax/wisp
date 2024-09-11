plugins {
    scala
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.scala-lang:scala3-library_3:3.5.0")
    implementation("org.slf4j:slf4j-api:2.0.16")

    testImplementation("org.scalatest:scalatest_3:3.2.19")
    testImplementation("org.slf4j:slf4j-simple:2.0.16")
}
