package com.sam.talkdraft.platform_capability.models

data class PlatformCpuModel(
    val coreCont: Int,
    val vendor: String,
    val arch: PlatformCpuModel.Arch,
) {

    enum class Arch {
        ARM64,
        ARM32,
        X86_64,
        X86,
        UNKNOWN
    }
}
