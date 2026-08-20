package com.sam.talkdraft.testing.annotations

@Target(allowedTargets = [AnnotationTarget.CLASS])
@Retention(value = AnnotationRetention.BINARY)
actual annotation class RunWithPlatform
