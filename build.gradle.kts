// Top-level build file
plugins {
    // 插件在app/build.gradle.kts中定义
}

tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}
