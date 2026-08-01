// Top-level build file
plugins {
    // 这里不需要添加插件，它们在app/build.gradle.kts中定义
}

allprojects {
    repositories {
        google()
        mavenCentral()
    }
}

tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}
