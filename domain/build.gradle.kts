plugins {
  id("java-library")
}

dependencies {
  api("org.springframework.boot:spring-boot-starter-data-jpa")
  api("com.fasterxml.jackson.core:jackson-databind")
  runtimeOnly("com.mysql:mysql-connector-j")
}
