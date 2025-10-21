plugins { id("java") }

dependencies {
  implementation(project(":domain"))
  implementation("org.springframework.boot:spring-boot-starter-data-jpa")
}
