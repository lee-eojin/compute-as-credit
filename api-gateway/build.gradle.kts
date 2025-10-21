plugins {
  id("org.springframework.boot")
  id("io.spring.dependency-management")
  id("java")
}

dependencies {
  implementation(project(":shared"))
  implementation(project(":domain"))
  implementation(project(":billing"))
  implementation(project(":orchestrator"))
  implementation(project(":adapters-core"))
  implementation(project(":adapters-fake"))
  implementation("org.springframework.boot:spring-boot-starter-web")
  implementation("org.springframework.boot:spring-boot-starter-validation")
  implementation("org.springframework.boot:spring-boot-starter-data-jpa")
  implementation("org.springframework.boot:spring-boot-starter-amqp")
  implementation("org.flywaydb:flyway-core")
  implementation("org.flywaydb:flyway-mysql")
  implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.5.0")
  runtimeOnly("com.mysql:mysql-connector-j")

  testImplementation("org.springframework.boot:spring-boot-starter-test")
  testImplementation("org.testcontainers:junit-jupiter:1.20.1")
  testImplementation("org.testcontainers:mysql:1.20.1")
  testImplementation("org.testcontainers:rabbitmq:1.20.1")
}

dependencies {
  implementation("org.springframework.boot:spring-boot-starter-security")
  implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
}
