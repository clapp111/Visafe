import com.diffplug.spotless.LineEnding
import net.ltgt.gradle.errorprone.errorprone

plugins {
    java
    id("org.springframework.boot") version "4.1.2"
    id("io.spring.dependency-management") version "1.1.7"
    id("com.diffplug.spotless") version "8.11.0"
    id("net.ltgt.errorprone") version "5.2.0"
}

group = "com.visafe"
version = "0.0.1-SNAPSHOT"
description = "Visafe API server"

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}

repositories {
    mavenCentral()
}

dependencies {
    // 정적 분석 (컴파일 시 실행)
    errorprone("com.google.errorprone:error_prone_core:2.52.0")

    // Web / API
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:3.2.0")

    // Persistence
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    runtimeOnly("org.postgresql:postgresql")

    // Lombok / config metadata
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

    developmentOnly("org.springframework.boot:spring-boot-devtools")

    // Test
    testImplementation("org.springframework.boot:spring-boot-starter-actuator-test")
    testImplementation("org.springframework.boot:spring-boot-starter-data-jpa-test")
    testImplementation("org.springframework.boot:spring-boot-starter-validation-test")
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testRuntimeOnly("com.h2database:h2")
    testCompileOnly("org.projectlombok:lombok")
    testAnnotationProcessor("org.projectlombok:lombok")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release = 21
    options.compilerArgs.add("-parameters")
    options.errorprone.disable("MissingOverride")
}

tasks.named<JavaCompile>("compileTestJava") {
    options.errorprone {
        // @MockitoBean/@Mock 필드는 외부 호출을 막으려고 선언만 하고 검증에 쓰지 않는 경우가 많아 오탐이 발생
        disable("UnusedVariable")
        // 테스트 픽스처의 상대 시간 계산(now().plusDays(7) 등)은 타임존과 무관하게 동작
        disable("JavaTimeDefaultTimeZone")
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

// 실행 가능한 boot jar만 생성 (plain jar 비활성화)
tasks.named<Jar>("jar") {
    enabled = false
}

// 새 프로젝트라 처음부터 전체 파일을 검사한다. (spotlessCheck는 check/build에 포함)
spotless {
    lineEndings = LineEnding.UNIX

    java {
        target("src/*/java/**/*.java")

        palantirJavaFormat("2.103.0")
        removeUnusedImports()
        trimTrailingWhitespace()
        endWithNewline()
    }
}

// scripts/hooks 의 스크립트를 저장소의 .git/hooks 로 복사해 git 훅을 설치한다.
//  - pre-commit: backend Java 파일이 커밋에 포함되면 spotlessCheck 실행
//  - pre-push  : push할 커밋에 backend 변경이 있으면 compileJava·compileTestJava 실행 (Error Prone)
// 모노레포(Visafe/)라 .git은 상위 폴더에 있다. 클론 후 backend에서 `./gradlew installGitHook`을 한 번 실행한다.
// scripts/hooks 의 스크립트를 수정하면 다시 실행해야 한다.
val gitHooksDir: File? = generateSequence(rootDir) { it.parentFile }
    .map { it.resolve(".git") }
    .firstOrNull { it.isDirectory }
    ?.resolve("hooks")

tasks.register<Copy>("installGitHook") {
    group = "git hooks"
    description = "Installs pre-commit (spotlessCheck) and pre-push (Error Prone) hooks"

    val hooksDir = gitHooksDir
    onlyIf {
        val present = hooksDir != null
        if (!present) {
            logger.warn("[installGitHook] .git 폴더를 찾지 못해 훅 설치를 건너뜁니다. (git init 후 다시 실행)")
        }
        present
    }

    from(layout.projectDirectory.dir("scripts/hooks"))
    into(hooksDir ?: layout.buildDirectory.dir("tmp/git-hooks").get().asFile)
    filePermissions {
        unix("rwxr-xr-x")
    }

    doLast {
        logger.lifecycle("[installGitHook] Hooks installed into $hooksDir")
    }
}
