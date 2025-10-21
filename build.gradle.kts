import org.gradle.api.tasks.testing.logging.TestExceptionFormat

plugins {
  idea
  id("io.spring.dependency-management") version "1.1.6" apply false
  id("org.springframework.boot") version "3.3.3" apply false
}

allprojects {
  group = "com.yourco.compute"
  version = "0.1.0-SNAPSHOT"
  repositories { mavenCentral() }
}

subprojects {
  apply(plugin = "java")
  apply(plugin = "io.spring.dependency-management")

  the<io.spring.gradle.dependencymanagement.dsl.DependencyManagementExtension>().apply {
    imports {
      mavenBom("org.springframework.boot:spring-boot-dependencies:3.3.3")
    }
  }

  configure<JavaPluginExtension> {
    toolchain {
      languageVersion.set(JavaLanguageVersion.of(17))
    }
  }

  dependencies {
    "testImplementation"(platform("org.junit:junit-bom:5.10.3"))
    "testImplementation"("org.junit.jupiter:junit-jupiter")
  }

  tasks.withType<Test> {
    useJUnitPlatform()
    testLogging { exceptionFormat = TestExceptionFormat.FULL }
  }
}

project(":api-gateway") {
  apply(plugin = "org.springframework.boot")
}
