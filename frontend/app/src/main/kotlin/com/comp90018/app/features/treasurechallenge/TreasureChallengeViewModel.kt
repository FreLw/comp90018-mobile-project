package com.comp90018.app.features.treasurechallenge

import android.content.Context
import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.comp90018.app.contextengine.AndroidDeviceContextEngine
import com.comp90018.app.contextengine.DeviceContextEngine
import com.comp90018.app.contextengine.DeviceContextSnapshot
import com.comp90018.app.contextengine.challenge.ChallengeEvent
import com.comp90018.app.contextengine.challenge.ChallengeInstruction
import com.comp90018.app.contextengine.challenge.ChallengeProgress
import com.comp90018.app.contextengine.challenge.ChallengeRuleEvaluator
import com.comp90018.app.contextengine.challenge.RelicChallengeConfig
import com.comp90018.app.contextengine.challenge.RelicChallengeType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

fun interface MonotonicTimeSource {
    fun nowNanos(): Long
}

enum class ChallengeCameraState {
    NOT_REQUIRED,
    LOCKED,
    READY,
    CAPTURING,
    CAPTURED,
    ERROR,
}

data class TreasureChallengeUiState(
    val challengeId: String,
    val challengeType: RelicChallengeType,
    val title: String,
    val instruction: ChallengeInstruction = ChallengeInstruction.MOVE_CLOSER,
    val instructionText: String = "Move closer",
    val holdProgress: Double = 0.0,
    val actionReady: Boolean = false,
    val completed: Boolean = false,
    val angularErrorDegrees: Double? = null,
    val cameraState: ChallengeCameraState = ChallengeCameraState.NOT_REQUIRED,
    val capturedPhotoUri: String? = null,
    val cameraError: String? = null,
    val active: Boolean = false,
)

/**
 * Lifecycle-facing integration boundary. Sensor rules remain in [ChallengeRuleEvaluator]; this
 * class configures collection and converts its progress into gameplay-focused UI state.
 */
class TreasureChallengeViewModel(
    initialConfig: RelicChallengeConfig,
    engineFactory: (CoroutineScope) -> DeviceContextEngine,
    private val timeSource: MonotonicTimeSource,
    scopeOverride: CoroutineScope? = null,
) : ViewModel() {
    private val collectionScope = scopeOverride ?: viewModelScope
    private val contextEngine = engineFactory(collectionScope)
    private var config = initialConfig
    private var evaluator = ChallengeRuleEvaluator(initialConfig)
    private var collectionJob: Job? = null
    private var isActive = false
    private var photoOpportunityReady = false
    private var photoReadySnapshot: DeviceContextSnapshot? = null

    private val mutableUiState = MutableStateFlow(initialUiState(initialConfig))
    val uiState: StateFlow<TreasureChallengeUiState> = mutableUiState.asStateFlow()

    fun start() {
        if (isActive) return
        isActive = true
        evaluator.reset()
        clearTransientState()
        mutableUiState.value = initialUiState(config).copy(active = true)
        contextEngine.setTargetLocation(config.targetLocation)
        contextEngine.setChallengeTargetHeading(config.requiredHeadingDegrees)
        collectionJob = contextEngine.output
            .onEach(::onSnapshot)
            .launchIn(collectionScope)
        contextEngine.start()
    }

    fun stop() {
        collectionJob?.cancel()
        collectionJob = null
        isActive = false
        contextEngine.stop()
        evaluator.reset()
        clearTransientState()
        mutableUiState.value = initialUiState(config)
    }

    fun restart() {
        val shouldStart = isActive
        stop()
        if (shouldStart) start()
    }

    /** Switching always creates a fresh evaluator so timing, readiness, and completion cannot leak. */
    fun activateChallenge(newConfig: RelicChallengeConfig) {
        val shouldStart = isActive
        stop()
        config = newConfig
        evaluator = ChallengeRuleEvaluator(newConfig)
        mutableUiState.value = initialUiState(newConfig)
        if (shouldStart) start()
    }

    fun onPhotoCaptureStarted() {
        if (!mutableUiState.value.actionReady || mutableUiState.value.completed) return
        mutableUiState.value = mutableUiState.value.copy(
            cameraState = ChallengeCameraState.CAPTURING,
            cameraError = null,
        )
    }

    fun onPhotoCaptured(uri: String) {
        if (!photoOpportunityReady || uri.isBlank() || mutableUiState.value.completed) return
        mutableUiState.value = mutableUiState.value.copy(
            cameraState = ChallengeCameraState.CAPTURED,
            capturedPhotoUri = uri,
            cameraError = null,
        )
        val readySnapshot = photoReadySnapshot ?: return
        val progress = evaluator.evaluate(readySnapshot, timeSource.nowNanos(), ChallengeEvent.PHOTO_CAPTURED)
        publish(progress, readySnapshot)
    }

    fun onCameraError(message: String) {
        if (!config.photoActionRequired || mutableUiState.value.completed) return
        mutableUiState.value = mutableUiState.value.copy(
            cameraState = ChallengeCameraState.ERROR,
            cameraError = message,
        )
    }

    override fun onCleared() {
        stop()
    }

    private fun onSnapshot(snapshot: DeviceContextSnapshot) {
        // Once TAKE_PHOTO has legitimately been reached, preserve that opportunity through the
        // physical shutter interaction. Completion still comes only from the evaluator event.
        if (photoOpportunityReady && config.photoActionRequired && !mutableUiState.value.completed) return
        val progress = evaluator.evaluate(snapshot, timeSource.nowNanos())
        publish(progress, snapshot)
    }

    private fun publish(progress: ChallengeProgress, snapshot: DeviceContextSnapshot) {
        if (progress.actionReady && config.photoActionRequired) {
            photoOpportunityReady = true
            photoReadySnapshot = snapshot
        }
        val completed = progress.completed
        val actionReady = photoOpportunityReady && !completed
        val instruction = when {
            completed -> ChallengeInstruction.COMPLETED
            actionReady -> ChallengeInstruction.TAKE_PHOTO
            else -> progress.instruction
        }
        val previous = mutableUiState.value
        mutableUiState.value = previous.copy(
            instruction = instruction,
            instructionText = instruction.displayText(config.type),
            holdProgress = if (actionReady || completed) 1.0 else progress.holdProgress,
            actionReady = actionReady,
            completed = completed,
            angularErrorDegrees = snapshot.orientation.direction.angularErrorDegrees,
            cameraState = when {
                !config.photoActionRequired -> ChallengeCameraState.NOT_REQUIRED
                completed && previous.capturedPhotoUri != null -> ChallengeCameraState.CAPTURED
                actionReady && previous.cameraState == ChallengeCameraState.CAPTURING -> ChallengeCameraState.CAPTURING
                actionReady && previous.cameraState == ChallengeCameraState.ERROR -> ChallengeCameraState.ERROR
                actionReady -> ChallengeCameraState.READY
                else -> ChallengeCameraState.LOCKED
            },
        )
    }

    private fun clearTransientState() {
        photoOpportunityReady = false
        photoReadySnapshot = null
    }

    companion object {
        fun factory(context: Context, config: RelicChallengeConfig) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = TreasureChallengeViewModel(
                initialConfig = config,
                engineFactory = { scope -> AndroidDeviceContextEngine(context.applicationContext, scope) },
                timeSource = MonotonicTimeSource(SystemClock::elapsedRealtimeNanos),
            ) as T
        }
    }
}

private fun initialUiState(config: RelicChallengeConfig) = TreasureChallengeUiState(
    challengeId = config.challengeId,
    challengeType = config.type,
    title = config.type.title(),
    cameraState = if (config.photoActionRequired) ChallengeCameraState.LOCKED else ChallengeCameraState.NOT_REQUIRED,
)

private fun RelicChallengeType.title(): String = when (this) {
    RelicChallengeType.UNION_LAWN_PHOTO -> "Union Lawn Lost Lake"
    RelicChallengeType.WILSON_HALL_OBSERVATION -> "Wilson Hall Observation"
    RelicChallengeType.OLD_QUAD_EXCAVATION -> "Old Quad Excavation"
    RelicChallengeType.SOUTH_LAWN_VIEWING_ANGLE -> "South Lawn Atlas"
    RelicChallengeType.SYSTEM_GARDEN_GLASSHOUSE -> "System Garden Glasshouse"
    RelicChallengeType.GRAINGER_MUSEUM_TONE_TOOL -> "Grainger Museum Tone-Tool"
}

private fun ChallengeInstruction.displayText(type: RelicChallengeType): String = when (this) {
    ChallengeInstruction.MOVE_CLOSER -> "Move closer"
    ChallengeInstruction.KEEP_PHONE_LEVEL -> "Keep your phone level"
    ChallengeInstruction.FIND_VIEWING_DIRECTION -> "Find the viewing direction"
    ChallengeInstruction.TURN_LEFT -> "Turn left"
    ChallengeInstruction.TURN_RIGHT -> "Turn right"
    ChallengeInstruction.STOP_MOVING -> if (type == RelicChallengeType.OLD_QUAD_EXCAVATION) "Hold still" else "Stop moving"
    ChallengeInstruction.HOLD_STILL -> if (type == RelicChallengeType.OLD_QUAD_EXCAVATION) "Hold still" else "Hold your phone steady"
    ChallengeInstruction.HOLD_ALIGNMENT -> "Hold this direction"
    ChallengeInstruction.HOLD_OBSERVATION -> "Keep observing"
    ChallengeInstruction.HOLD_EXCAVATION_POSITION -> "Excavating"
    ChallengeInstruction.HOLD_VIEWING_ANGLE -> "Locking angle"
    ChallengeInstruction.HOLD_GLASSHOUSE_POSITION -> "Excavating"
    ChallengeInstruction.MAKE_SOUND -> "Make some noise"
    ChallengeInstruction.TAKE_PHOTO -> "Take photo"
    ChallengeInstruction.COMPLETED -> "Relic discovered"
}
