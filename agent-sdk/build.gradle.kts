plugins { id("java") }

dependencies {
  implementation("org.springframework:spring-web")
  implementation("com.fasterxml.jackson.core:jackson-databind")

  testImplementation("org.springframework:spring-test")
  testImplementation("org.springframework:spring-core")
  testImplementation("org.assertj:assertj-core")
  testImplementation("org.hamcrest:hamcrest")
}
