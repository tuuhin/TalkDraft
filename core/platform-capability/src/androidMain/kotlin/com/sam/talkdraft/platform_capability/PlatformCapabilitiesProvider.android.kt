package com.sam.talkdraft.platform_capability

import android.app.ActivityManager
import android.app.usage.StorageStatsManager
import android.content.Context
import android.os.Build
import android.os.storage.StorageManager
import androidx.core.content.getSystemService
import co.touchlab.kermit.Logger
import com.sam.talkdraft.common.platform.IPlatformCoroutineDispatchers
import com.sam.talkdraft.platform_capability.models.PlatformCapabilities
import com.sam.talkdraft.platform_capability.models.PlatformCpuModel
import com.sam.talkdraft.platform_capability.models.PlatformMemoryModel
import com.sam.talkdraft.platform_capability.models.PlatformOS
import com.sam.talkdraft.platform_capability.models.PlatformStorageModel
import com.sam.talkdraft.platform_capability.models.SupportedOS
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.koin.core.annotation.Factory

private const val TAG = "PlatformCapabilities"

@Factory(binds = [IPlatformCapabilitiesProvider::class])
internal actual class PlatformCapabilitiesProvider(
    private val context: Context,
    private val dispatchers: IPlatformCoroutineDispatchers,
) : IPlatformCapabilitiesProvider {

    private val storageManager by lazy { context.getSystemService<StorageStatsManager>() }
    private val activityManager by lazy { context.getSystemService<ActivityManager>() }

    actual override suspend fun getCapabilities(): Result<PlatformCapabilities> {
        return coroutineScope {
            runCatching {
                val os = PlatformOS(os = SupportedOS.ANDROID, version = Build.VERSION.SDK_INT.toString())
                val storage = async { readStorageStats() }
                val cpu = readCpuStatus()
                val memory = readMemory()
                PlatformCapabilities(
                    memory = memory,
                    storage = storage.await(),
                    processor = cpu,
                    operatingSystem = os,
                )
            }
        }
    }

    private suspend fun readStorageStats(): PlatformStorageModel {
        return withContext(dispatchers.io) {
            try {
                val totalBytes = async { storageManager?.getTotalBytes(StorageManager.UUID_DEFAULT) ?: 0L }
                val freeBytes = async { storageManager?.getFreeBytes(StorageManager.UUID_DEFAULT) ?: 0L }
                PlatformStorageModel(totalBytes = totalBytes.await(), availableBytes = freeBytes.await())

            } catch (e: SecurityException) {
                Logger.e(throwable = e, tag = TAG) { "FAILED TO READ DEVICE STORAGE" }
                PlatformStorageModel(totalBytes = 0L, availableBytes = 0L)
            }
        }
    }

    private fun readCpuStatus(): PlatformCpuModel {

        val noOfCores = Runtime.getRuntime().availableProcessors()
        val vendor = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) Build.SOC_MODEL.ifBlank { Build.HARDWARE }
        else Build.HARDWARE

        val primaryAbi = Build.SUPPORTED_ABIS.firstOrNull() ?: ""
        val arch = primaryAbi.abiToArch()

        return PlatformCpuModel(
            coreCont = noOfCores,
            vendor = vendor,
            arch = arch,
        )
    }

    private fun readMemory(): PlatformMemoryModel {
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager?.getMemoryInfo(memoryInfo)
        return PlatformMemoryModel(
            totalBytes = memoryInfo.totalMem,
            availableBytes = memoryInfo.availMem,
        )
    }

    private fun String.abiToArch(): PlatformCpuModel.Arch {
        return when {
            startsWith("arm64") -> PlatformCpuModel.Arch.ARM64
            startsWith("armeabi") -> PlatformCpuModel.Arch.ARM32
            equals("x86_64", ignoreCase = true) -> PlatformCpuModel.Arch.X86_64
            equals("x86", ignoreCase = true) -> PlatformCpuModel.Arch.X86
            else -> PlatformCpuModel.Arch.UNKNOWN
        }
    }
}
