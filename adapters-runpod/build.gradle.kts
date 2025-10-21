plugins { id("java") }

dependencies {
  implementation(project(":adapters-core"))
  implementation("org.springframework.boot:spring-boot-starter-web")
  testImplementation("com.github.tomakehurst:wiremock-jre8:2.35.1")
  testImplementation("javax.servlet:javax.servlet-api:4.0.1")
}
