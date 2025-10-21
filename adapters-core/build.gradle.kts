plugins { id("java-library") }

dependencies {
  api(project(":domain"))
  implementation("org.springframework.boot:spring-boot-starter")
}
