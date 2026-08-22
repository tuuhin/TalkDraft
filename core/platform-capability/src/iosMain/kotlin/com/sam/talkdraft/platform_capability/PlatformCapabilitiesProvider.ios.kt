package com.sam.talkdraft.platform_capability

import com.sam.talkdraft.common.platform.IPlatformCoroutineDispatchers
import com.sam.talkdraft.platform_capability.models.PlatformCapabilities
import com.sam.talkdraft.platform_capability.models.PlatformCpuModel
import com.sam.talkdraft.platform_capability.models.PlatformMemoryModel
import com.sam.talkdraft.platform_capability.models.PlatformOS
import com.sam.talkdraft.platform_capability.models.PlatformStorageModel
import com.sam.talkdraft.platform_capability.models.SupportedOS
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.sizeOf
import kotlinx.cinterop.usePinned
import kotlinx.cinterop.value
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import org.koin.core.annotation.Factory
import platform.Foundation.NSHomeDirectory
import platform.Foundation.NSProcessInfo
import platform.Foundation.NSURL
import platform.Foundation.NSURLVolumeAvailableCapacityForImportantUsageKey
import platform.Foundation.NSURLVolumeTotalCapacityKey
import platform.darwin.sysctlbyname
import platform.posix.uint64_tVar

@OptIn(ExperimentalForeignApi::class)
@Factory(binds = [IPlatformCapabilitiesProvider::class])
internal actual class PlatformCapabilitiesProvider(
    private val dispatchers: IPlatformCoroutineDispatchers,
) : IPlatformCapabilitiesProvider {

    actual override suspend fun getCapabilities(): Result<PlatformCapabilities> {
        return runCatching {
            coroutineScope {
                val os = PlatformOS(
                    os = SupportedOS.IOS,
                    version = NSProcessInfo.processInfo.operatingSystemVersionString,
                )

                val storageDeferred = async(dispatchers.io) { readStorageStats() }
                val cpu = readCpuStatus()
                val memory = readMemory()

                PlatformCapabilities(
                    memory = memory,
                    storage = storageDeferred.await(),
                    processor = cpu,
                    operatingSystem = os,
                )
            }
        }
    }

    private fun readMemory(): PlatformMemoryModel {
        val totalBytes = NSProcessInfo.processInfo.physicalMemory.toLong()
        val availableBytes = getSystemSysctlInt64("hw.usermem") ?: 0L
        return PlatformMemoryModel(
            totalBytes = totalBytes,
            availableBytes = availableBytes,
        )
    }


    private fun readStorageStats(): PlatformStorageModel {
        return try {
            val fileURL = NSURL.fileURLWithPath(NSHomeDirectory())
            val values = fileURL.resourceValuesForKeys(
                listOf(NSURLVolumeTotalCapacityKey, NSURLVolumeAvailableCapacityForImportantUsageKey),
                error = null,
            )

            val totalBytes = (values?.get(NSURLVolumeTotalCapacityKey) as? Number)?.toLong() ?: 0L
            val availableBytes =
                (values?.get(NSURLVolumeAvailableCapacityForImportantUsageKey) as? Number)?.toLong() ?: 0L

            PlatformStorageModel(totalBytes = totalBytes, availableBytes = availableBytes)
        } catch (e: Exception) {
            PlatformStorageModel(totalBytes = 0L, availableBytes = 0L)
        }
    }

    private fun readCpuStatus(): PlatformCpuModel {
        val cores = NSProcessInfo.processInfo.processorCount.toInt()
        val arch = getIosArchitecture()

        val machineName = getSystemSysctlString("hw.machine") ?: "Apple"

        return PlatformCpuModel(
            coreCont = cores,
            vendor = machineName,
            arch = arch,
        )
    }

    private fun getIosArchitecture(): PlatformCpuModel.Arch {
        val machine = getSystemSysctlString("hw.machine")?.lowercase() ?: ""
        return when {
            machine.contains("x86_64") -> PlatformCpuModel.Arch.X86_64
            machine.contains("i386") -> PlatformCpuModel.Arch.X86
            machine.contains("arm64") || machine.contains("iphone") || machine.contains("ipad") -> PlatformCpuModel.Arch.ARM64
            machine.contains("arm") -> PlatformCpuModel.Arch.ARM32
            else -> PlatformCpuModel.Arch.ARM64
        }
    }


    private fun getSystemSysctlInt64(key: String): Long? = memScoped {
        val size = alloc<uint64_tVar>().apply {
            value = sizeOf<uint64_tVar>().toULong()
        }
        val result = alloc<uint64_tVar>()
        if (sysctlbyname(key, result.ptr, size.ptr, null, 0UL) == 0) {
            result.value.toLong()
        } else {
            null
        }
    }

    private fun getSystemSysctlString(key: String): String? = memScoped {
        // set the pointer
        val size = alloc<uint64_tVar>()
        // a empty to indent how long is the output
        sysctlbyname(key, null, size.ptr, null, 0UL)
        if (size.value == 0UL) return null

        val buffer = ByteArray(size.value.toInt())
        buffer.usePinned { pinned ->
            // corrected call to read the values
            if (sysctlbyname(key, pinned.addressOf(0), size.ptr, null, 0UL) == 0)
                buffer.decodeToString().trimEnd('\u0000')
            else null
        }
    }
}
