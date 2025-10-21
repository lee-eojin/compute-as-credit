plugins { id("java") }

dependencies {
  implementation("org.springframework.boot:spring-boot-starter")
  implementation("org.springframework.boot:spring-boot-starter-amqp")
  implementation("io.micrometer:micrometer-observation")
}
