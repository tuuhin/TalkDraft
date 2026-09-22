package com.sam.talkdraft.ios_archive

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCObjectVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import okio.Path
import platform.Foundation.NSError
import swiftPMImport.TalkDraft.core.core.archive.ios.IosNativeTar

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
class NativeTarArchiver {

    fun createTar(srcDirectory: Path, destPath: Path): Result<Unit> = runCatching {
        memScoped {
            val error = alloc<ObjCObjectVar<NSError?>>()
            val isSuccess =
                IosNativeTar.createTarFromDirectory(
                    srcDirectory.toString(),
                    destination = destPath.toString(),
                    error.ptr,
                )

            if (isSuccess) return@runCatching
            throw IllegalStateException(error.value?.localizedDescription ?: "Failed to create aTAR archive")
        }
    }

    fun extractTar(srcPath: Path, destPath: Path): Result<Unit> = runCatching {
        memScoped {
            val error = alloc<ObjCObjectVar<NSError?>>()
            val isSuccess =
                IosNativeTar.extractTar(srcPath.toString(), destination = destPath.toString(), error.ptr)

            if (isSuccess) return@runCatching
            throw IllegalStateException(error.value?.localizedDescription ?: "Failed to extract TAR archive")
        }
    }
}
