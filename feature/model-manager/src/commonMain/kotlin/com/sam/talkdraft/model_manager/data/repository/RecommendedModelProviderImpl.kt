package com.sam.talkdraft.model_manager.data.repository

import com.sam.talkdraft.model_manager.domain.model.TranscriptionModel
import com.sam.talkdraft.model_manager.domain.repository.IRecommendedModelProvider
import com.sam.talkdraft.model_manager.domain.repository.ITranscriptionModelsRepo
import com.sam.talkdraft.platform_capability.IPlatformCapabilitiesProvider
import com.sam.talkdraft.platform_capability.models.PlatformCapabilities
import org.koin.core.annotation.Factory

@Factory(binds = [IRecommendedModelProvider::class])
internal class RecommendedModelProviderImpl(
    private val provider: IPlatformCapabilitiesProvider,
    private val repository: ITranscriptionModelsRepo,
) : IRecommendedModelProvider {

    override suspend fun recommendedModel(): Result<TranscriptionModel> {
        return runCatching {

            val capabilities = provider.getCapabilities().getOrThrow()
            val recommendedModel = repository.readAllModels().getOrThrow()
                .firstOrNull { canRunLargeModel(it, capabilities) }

            val fallback = repository.readSmallestModel(readEmptySpace())
                .getOrThrow()
            recommendedModel ?: fallback
        }
    }

    private fun canRunLargeModel(model: TranscriptionModel, capabilities: PlatformCapabilities): Boolean {
        return capabilities.memory.totalBytes >= MEDIUM_MIN_MEMORY_BYTES &&
            capabilities.storage.availableBytes >= model.sizeInBytes
    }

    private suspend fun readEmptySpace(): Long {
        val capabilities = provider.getCapabilities().getOrThrow()
        return capabilities.storage.availableBytes
    }

    companion object {
        private const val MEDIUM_MIN_MEMORY_BYTES = 4L * 1024 * 1024 * 1024
    }
}
