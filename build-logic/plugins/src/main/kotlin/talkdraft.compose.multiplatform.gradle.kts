plugins {
    id("talkdraft.kmp.library")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
}

composeCompiler {
    metricsDestination = layout.buildDirectory.dir("compose_compiler")
    reportsDestination = layout.buildDirectory.dir("compose_compiler")
    stabilityConfigurationFiles.add(rootProject.layout.projectDirectory.file("stability_config.conf"))
}
