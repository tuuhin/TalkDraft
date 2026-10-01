package com.sam.talkdraft.model_manager.data.repository

import com.sam.talkdraft.model_manager.domain.model.TranscriptionModel
import com.sam.talkdraft.model_manager.domain.model.TranscriptionType
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

    override suspend fun recommendedModel(type: TranscriptionType): Result<TranscriptionModel> {
        return runCatching {

            // read in the capabilities
            val capabilities = provider.getCapabilities().getOrThrow()
            // read all the recommended model
            val recommendedModel = repository.readAllModelByType(type).getOrThrow()
                .sortedBy { it.sizeInBytes }
                .firstOrNull { canRunLargeModel(it, capabilities) }

            // if recommended is  good then allow
            if (recommendedModel != null) return@runCatching recommendedModel

            // otherwise fallback to model based on storage
            val availableStorage = readEmptySpace()
            return@runCatching repository.readSmallestModel(maxModelSize = availableStorage, type = type)
                .getOrThrow()
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
        // 3GB RAM requirements for now
        private const val MEDIUM_MIN_MEMORY_BYTES = 3L * 1024 * 1024 * 1024
    }
}
