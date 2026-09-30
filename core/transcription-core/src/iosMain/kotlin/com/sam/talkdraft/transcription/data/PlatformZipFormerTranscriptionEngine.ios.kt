@file:OptIn(ExperimentalAtomicApi::class)

package com.sam.talkdraft.transcription.data

import co.touchlab.kermit.Logger
import com.sam.talkdraft.common.platform.IPlatformCoroutineDispatchers
import com.sam.talkdraft.transcription.data.models.ZipFormerModelPath
import com.sam.talkdraft.transcription.domain.ITranscriptionEngine
import com.sam.talkdraft.transcription.domain.model.TranscriberConfig
import com.sam.talkdraft.transcription.domain.model.TranscriptionSegmentModel
import com.sam.talkdraft.transcription.domain.model.TranscriptionState
import com.sam.talkdraft.transcription.ios.IosNativeZipFormer
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.AtomicLong
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import org.koin.core.annotation.Factory
import org.koin.core.annotation.Named

@Factory(binds = [ITranscriptionEngine::class])
@Named(value = "zip_former_engine")
internal actual class PlatformZipFormerTranscriptionEngine(
    private val dispatcher: IPlatformCoroutineDispatchers,
) : ITranscriptionEngine {

    private val instance by lazy { IosNativeZipFormer() }
    private val _isSetupDone = AtomicBoolean(false)
    private val _lock = Mutex()

    private val oldSegmentId = AtomicLong(1L)
    private val _activeSegmentBuilder = StringBuilder()

    private val _completedSegments = mutableListOf<TranscriptionSegmentModel>()
    private val _finalizedText = StringBuilder()

    private val fs = FileSystem.SYSTEM

    actual override suspend fun warmUp(request: TranscriberConfig) {
        _lock.withLock {
            if (_isSetupDone.load()) {
                Logger.w(tag = TAG) { "WARMUP IS ALREADY COMPLETED" }
                return
            }
            val success = withContext(dispatcher.default) {
                val modelPath = request.modelPath.toPath()
                // model file/folder itself is missing
                val fileMap = checkAndFetchFileMap(modelPath)
                instance.initialize(
                    encoderPath = fileMap.encoderPath,
                    decoderPath = fileMap.decoderPath,
                    joinerPath = fileMap.joinerPath,
                    tokensPath = fileMap.tokenPath,
                )
            }
            _isSetupDone.compareAndSet(expectedValue = false, success)
            Logger.d(tag = TAG) { "AUDIO TRANSCRIPTION SETUP COMPLETED :$success" }
        }
    }

    actual override fun processSegment(bytes: ShortArray, timeStamp: ClosedRange<Duration>): TranscriptionState {

        if (!_isSetupDone.load()) {
            Logger.w(tag = TAG) { "SETUP IS MISSING FIRST SET IT UP" }
            return TranscriptionState.Idle
        }

        Logger.d(tag = TAG) { "ENGINE INPUT SIZE :${bytes.size}" }

        val zipFormerResult = instance.processFrame(bytes)
            ?: run {
                // build the rest of the script but mark it as no result
                return TranscriptionState.Success(
                    text = buildFullTranscript(),
                    segments = getCurrentSegments(
                        activeSegmentId = oldSegmentId.load(),
                        startTime = timeStamp.start,
                        endTime = timeStamp.endInclusive,
                    ),
                )
            }

        val segmentId = zipFormerResult.segmentId
        val cumulativeHypothesis = zipFormerResult.text.trim()

        if (cumulativeHypothesis.isNotEmpty()) {
            val currentOldId = oldSegmentId.load()

            if (currentOldId != segmentId) {
                commitActiveSegment(currentOldId)
                oldSegmentId.store(segmentId)
            }

            _activeSegmentBuilder.setLength(0)
            _activeSegmentBuilder.append(cumulativeHypothesis)
        }

        return TranscriptionState.Success(
            text = buildFullTranscript(),
            segments = getCurrentSegments(
                activeSegmentId = segmentId,
                startTime = timeStamp.start,
                endTime = timeStamp.endInclusive,
            ),
        )
    }

    actual override fun reset() {
        if (!_isSetupDone.load()) return
        val currentId = oldSegmentId.load()
        commitActiveSegment(currentId)
        Logger.d(tag = TAG) { "TRANSCRIPTION ENGINE RESET COMPLETED FOR SEGMENT $currentId" }
    }

    actual override fun cleanUp() {
        if (_isSetupDone.compareAndSet(expectedValue = true, newValue = false)) {
            Logger.d(tag = TAG) { "AUDIO TRANSCRIPTION SETUP CLOSED" }
            instance.close()
        }

        _completedSegments.clear()
        _finalizedText.setLength(0)
        _activeSegmentBuilder.setLength(0)
        oldSegmentId.store(1L)
    }

    private fun commitActiveSegment(segmentId: Long) {
        if (_activeSegmentBuilder.isEmpty()) return

        val activeText = _activeSegmentBuilder.toString()

        if (_finalizedText.isNotEmpty()) _finalizedText.append(" ")
        _finalizedText.append(activeText)

        _completedSegments.add(TranscriptionSegmentModel(text = activeText, segmentId = segmentId))
        _activeSegmentBuilder.setLength(0)
    }

    private fun buildFullTranscript(): String {
        return when {
            _finalizedText.isEmpty() -> _activeSegmentBuilder.toString()
            _activeSegmentBuilder.isEmpty() -> _finalizedText.toString()
            else -> "$_finalizedText $_activeSegmentBuilder"
        }
    }

    private fun getCurrentSegments(
        activeSegmentId: Long,
        startTime: Duration = 0.seconds,
        endTime: Duration = 0.seconds,
    ): List<TranscriptionSegmentModel> {
        val totalSize = _completedSegments.size + if (_activeSegmentBuilder.isNotEmpty()) 1 else 0
        val resultList = ArrayList<TranscriptionSegmentModel>(totalSize)

        for (i in _completedSegments.indices)
            resultList.add(_completedSegments[i])

        if (_activeSegmentBuilder.isNotEmpty()) {
            resultList.add(
                TranscriptionSegmentModel(
                    text = _activeSegmentBuilder.toString(),
                    segmentId = activeSegmentId,
                    startTimeMs = startTime,
                    endTime = endTime,
                ),
            )
        }
        return resultList
    }

    private fun checkAndFetchFileMap(path: Path): ZipFormerModelPath {
        val targetNames = setOf("encoder", "decoder", "joiner", "tokens")

        val foundFiles = fs.listRecursively(path)
            .filter { fs.metadata(it).isRegularFile }
            .mapNotNull { path ->
                // matching based on the start name
                val fileName = path.name.lowercase()
                val matchedPrefix = targetNames.firstOrNull { prefix ->
                    fileName.startsWith(prefix)
                }
                matchedPrefix?.let { it to path }
            }
            .toMap()

        val encoderPath = foundFiles["encoder"] ?: throw IllegalArgumentException("Encoder file missing")
        val decoderPath = foundFiles["decoder"] ?: throw IllegalArgumentException("Decoder file missing")
        val joinerPath = foundFiles["joiner"] ?: throw IllegalArgumentException("Joiner filer missing")
        val tokensPath = foundFiles["tokens"] ?: throw IllegalArgumentException("Tokens file missing")

        val missingFiles = targetNames - foundFiles.keys
        if (missingFiles.isNotEmpty())
            throw IllegalStateException("Missing required model files: $missingFiles in $path")

        return ZipFormerModelPath(encoderPath, decoderPath, joinerPath, tokensPath)
    }

    companion object {
        private const val TAG = "ZIP_FORMER_TRANSCRIPTION_ENGINE"
    }
}
