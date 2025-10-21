plugins { id("java") }

dependencies {
  implementation(project(":domain"))
  implementation(project(":adapters-core"))
  implementation(project(":billing"))
  implementation(project(":shared"))
  implementation("org.springframework.boot:spring-boot-starter")
  implementation("io.github.resilience4j:resilience4j-spring-boot3:2.2.0")
}

dependencies {
  implementation("com.github.ben-manes.caffeine:caffeine:3.1.8")
  implementation("org.springframework.boot:spring-boot-starter-amqp")
  implementation("org.springframework.boot:spring-boot-starter-data-jpa")
  runtimeOnly("com.mysql:mysql-connector-j")
}
