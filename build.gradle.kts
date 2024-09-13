plugins {
    scala
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.scala-lang:scala3-library_3:3.5.0")

    testImplementation("org.scalatest:scalatest_3:3.2.19")
}
