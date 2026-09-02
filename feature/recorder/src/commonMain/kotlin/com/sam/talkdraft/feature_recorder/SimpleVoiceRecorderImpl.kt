package com.sam.talkdraft.feature_recorder

import com.sam.talkdraft.common.platform.IPlatformCoroutineDispatchers
import com.sam.talkdraft.model_manager.domain.repository.IRecommendedModelProvider
import com.sam.talkdraft.recorder.domain.IAudioVisualizerProvider
import com.sam.talkdraft.recorder.domain.IVoiceRecorderWithByteReader
import com.sam.talkdraft.recorder.domain.models.RecorderPoint
import com.sam.talkdraft.recorder.domain.models.RecorderState
import com.sam.talkdraft.transcription.domain.ITranscriptionEngine
import com.sam.talkdraft.transcription.domain.model.TranscriptionRequestMetadata
import com.sam.talkdraft.transcription.domain.model.TranscriptionState
import kotlin.time.Duration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.withContext
import org.koin.core.annotation.Factory
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf

@Factory(binds = [ISimpleVoiceRecorder::class])
class SimpleVoiceRecorderImpl(
    private val dispatchers: IPlatformCoroutineDispatchers,
    private val transcriptionEngine: ITranscriptionEngine,
    private val recommendedModel: IRecommendedModelProvider,
) : ISimpleVoiceRecorder, KoinComponent {

    private val _scope = CoroutineScope(dispatchers.default + SupervisorJob())

    private val _recorder: IVoiceRecorderWithByteReader by inject(parameters = { parametersOf(_scope) })
    private val _ampsReader: IAudioVisualizerProvider by inject(parameters = { parametersOf(_recorder) })

    override val state: StateFlow<RecorderState> = _recorder.state
    override val elapsedTime: StateFlow<Duration> = _recorder.elapsedTime

    override val timeLine: Flow<Sequence<RecorderPoint>> = _ampsReader.dataPoints

    @OptIn(ExperimentalCoroutinesApi::class)
    override val transcription: Flow<TranscriptionState>
        get() = _recorder.stream.mapLatest {
            transcriptionEngine.process(it.toShortArray())
        }.flowOn(dispatchers.default)

    override val errors: Flow<Exception>
        field = MutableSharedFlow<Exception>()

    override suspend fun setup() {
        withContext(dispatchers.io) {
            val model = recommendedModel.recommendedModel()
                .getOrNull() ?: return@withContext

            val modelPath = model.modelPath ?: return@withContext

            val request = TranscriptionRequestMetadata(modelPath, null)
            transcriptionEngine.warmUp(request)
        }
    }

    override suspend fun stop() {
        val newPath = withContext(dispatchers.io) { _recorder.stop() }

    }

    override suspend fun start() = withContext(dispatchers.io) { _recorder.start() }
    override suspend fun pause() = _recorder.pause()
    override suspend fun resume() = _recorder.resume()
    override suspend fun cancel() = _recorder.cancel()


    override fun close() {
        transcriptionEngine.cleanUp()
        _recorder.release()
        _scope.cancel()
    }

}
