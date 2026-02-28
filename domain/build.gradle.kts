plugins {
  id("java-library")
}

dependencies {
  api("org.springframework.boot:spring-boot-starter-data-jpa")
  api("com.fasterxml.jackson.core:jackson-databind")
  implementation("org.springframework.boot:spring-boot-starter")
  implementation("org.springframework.boot:spring-boot-starter-amqp")
  implementation("io.micrometer:micrometer-observation")
  runtimeOnly("com.mysql:mysql-connector-j")
}
