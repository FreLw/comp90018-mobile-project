package com.comp90018.app.contextengine.challenge

import com.comp90018.app.contextengine.DeviceContextSnapshot
import com.comp90018.app.sensors.DirectionAlignment
import com.comp90018.app.sensors.DirectionOutput
import com.comp90018.app.sensors.DirectionProcessor
import com.comp90018.app.sensors.HorizontalState
import com.comp90018.app.sensors.MotionState
import com.comp90018.app.sensors.RotationState
import com.comp90018.app.sensors.SensorValidity
import com.comp90018.app.sensors.StabilityState
import com.comp90018.app.sensors.TurnDirection
import com.comp90018.app.sensors.location.GeoCoordinate
import com.comp90018.app.sensors.location.LocationCalculator

/**
 * Deterministic challenge state machine. It owns only timing/completion state and has no Android,
 * Compose, coroutine, or wall-clock dependency. Create one evaluator per active challenge.
 */
class ChallengeRuleEvaluator(private val config: RelicChallengeConfig) {
    private var holdStartedAtNanos: Long? = null
    private var lastEvaluationNanos: Long? = null
    private var isCompleted = false

    fun evaluate(
        snapshot: DeviceContextSnapshot,
        nowNanos: Long,
        event: ChallengeEvent? = null,
    ): ChallengeProgress {
        require(nowNanos >= 0L)
        require(lastEvaluationNanos == null || nowNanos >= lastEvaluationNanos!!) {
            "nowNanos must be monotonic"
        }
        lastEvaluationNanos = nowNanos

        val direction = evaluateDirection(snapshot)
        val conditions = buildConditionStates(snapshot, direction)
        val allConditionsSatisfied = conditions.all { it.satisfied }

        if (!allConditionsSatisfied) {
            holdStartedAtNanos = null
        } else if (holdStartedAtNanos == null) {
            holdStartedAtNanos = nowNanos
        }

        val holdProgress = when {
            isCompleted -> 1.0
            !allConditionsSatisfied -> 0.0
            config.holdDurationNanos == 0L -> 1.0
            else -> ((nowNanos - requireNotNull(holdStartedAtNanos)).toDouble() /
                config.holdDurationNanos.toDouble()).coerceIn(0.0, 1.0)
        }
        val holdComplete = allConditionsSatisfied && holdProgress >= 1.0
        val actionReady = !isCompleted && config.photoActionRequired && holdComplete

        if (!isCompleted && holdComplete && !config.photoActionRequired) {
            isCompleted = true
        }
        if (!isCompleted && event == ChallengeEvent.PHOTO_CAPTURED && actionReady) {
            isCompleted = true
        }

        return ChallengeProgress(
            challengeId = config.challengeId,
            challengeType = config.type,
            requiredConditions = conditions,
            instruction = instruction(conditions, direction, holdComplete),
            holdProgress = if (isCompleted) 1.0 else holdProgress,
            actionReady = actionReady && !isCompleted,
            completed = isCompleted,
        )
    }

    fun reset() {
        holdStartedAtNanos = null
        lastEvaluationNanos = null
        isCompleted = false
    }

    private fun buildConditionStates(
        snapshot: DeviceContextSnapshot,
        direction: DirectionOutput?,
    ): List<ChallengeConditionState> = buildList {
        add(ChallengeConditionState(ChallengeCondition.LOCATION_INSIDE, isInsideTarget(snapshot)))
        if (config.requiresHorizontal) {
            add(ChallengeConditionState(
                ChallengeCondition.PHONE_HORIZONTAL,
                snapshot.orientation.attitude.validity == SensorValidity.VALID &&
                    snapshot.orientation.attitude.horizontalState == HorizontalState.HORIZONTAL,
            ))
        }
        if (config.requiredHeadingDegrees != null) {
            add(ChallengeConditionState(
                ChallengeCondition.HEADING_ALIGNED,
                direction?.comparisonValidity == SensorValidity.VALID &&
                    direction.alignment == DirectionAlignment.ALIGNED,
            ))
        }
        if (config.requiresStationary) {
            add(ChallengeConditionState(
                ChallengeCondition.STATIONARY,
                snapshot.motionStability.motion.validity == SensorValidity.VALID &&
                    snapshot.motionStability.motion.classification == MotionState.STATIONARY,
            ))
        }
        if (config.requiresStability) {
            add(ChallengeConditionState(
                ChallengeCondition.STABLE,
                snapshot.motionStability.stability.validity == SensorValidity.VALID &&
                    snapshot.motionStability.stability.classification == StabilityState.STABLE,
            ))
        }
        if (config.requiresRotationStill) {
            add(ChallengeConditionState(
                ChallengeCondition.ROTATION_STILL,
                snapshot.orientation.rotation.validity == SensorValidity.VALID &&
                    snapshot.orientation.rotation.classification == RotationState.STILL,
            ))
        }
        if (config.requiresSound) {
            val threshold = requireNotNull(config.soundThresholdDecibels)
            add(ChallengeConditionState(
                ChallengeCondition.SOUND_DETECTED,
                snapshot.sound.validity == SensorValidity.VALID &&
                    (snapshot.sound.decibels ?: Double.NEGATIVE_INFINITY) >= threshold,
            ))
        }
    }

    private fun evaluateDirection(snapshot: DeviceContextSnapshot): DirectionOutput? {
        val target = config.requiredHeadingDegrees ?: return null
        val heading = snapshot.orientation.direction.headingDegrees
            .takeIf { snapshot.orientation.direction.headingValidity == SensorValidity.VALID }
        return DirectionProcessor.evaluate(heading, target, config.headingToleranceDegrees)
    }

    private fun isInsideTarget(snapshot: DeviceContextSnapshot): Boolean {
        if (snapshot.location.validity != SensorValidity.VALID) return false
        val current = snapshot.location.currentLocation ?: return false
        if (!current.isValid()) return false
        return LocationCalculator.distanceMeters(current, config.targetLocation) <= config.insideRadiusMeters
    }

    private fun instruction(
        conditions: List<ChallengeConditionState>,
        direction: DirectionOutput?,
        holdComplete: Boolean,
    ): ChallengeInstruction {
        if (isCompleted) return ChallengeInstruction.COMPLETED
        if (!conditions.isSatisfied(ChallengeCondition.LOCATION_INSIDE)) return ChallengeInstruction.MOVE_CLOSER
        if (!conditions.isSatisfied(ChallengeCondition.PHONE_HORIZONTAL)) return ChallengeInstruction.KEEP_PHONE_LEVEL
        if (!conditions.isSatisfied(ChallengeCondition.HEADING_ALIGNED)) {
            return when (direction?.turn) {
                TurnDirection.TURN_LEFT -> ChallengeInstruction.TURN_LEFT
                TurnDirection.TURN_RIGHT -> ChallengeInstruction.TURN_RIGHT
                else -> ChallengeInstruction.FIND_VIEWING_DIRECTION
            }
        }
        if (!conditions.isSatisfied(ChallengeCondition.STATIONARY)) return ChallengeInstruction.STOP_MOVING
        if (!conditions.isSatisfied(ChallengeCondition.STABLE) ||
            !conditions.isSatisfied(ChallengeCondition.ROTATION_STILL)
        ) return ChallengeInstruction.HOLD_STILL
        if (!conditions.isSatisfied(ChallengeCondition.SOUND_DETECTED)) return ChallengeInstruction.MAKE_SOUND
        if (holdComplete && config.photoActionRequired) return ChallengeInstruction.TAKE_PHOTO
        return when (config.type) {
            RelicChallengeType.UNION_LAWN_PHOTO -> ChallengeInstruction.HOLD_ALIGNMENT
            RelicChallengeType.WILSON_HALL_OBSERVATION -> ChallengeInstruction.HOLD_OBSERVATION
            RelicChallengeType.OLD_QUAD_EXCAVATION -> ChallengeInstruction.HOLD_EXCAVATION_POSITION
            RelicChallengeType.SOUTH_LAWN_VIEWING_ANGLE -> ChallengeInstruction.HOLD_VIEWING_ANGLE
            RelicChallengeType.SYSTEM_GARDEN_GLASSHOUSE -> ChallengeInstruction.HOLD_GLASSHOUSE_POSITION
            RelicChallengeType.GRAINGER_MUSEUM_TONE_TOOL -> ChallengeInstruction.HOLD_TONE
        }
    }

    private fun List<ChallengeConditionState>.isSatisfied(condition: ChallengeCondition): Boolean =
        firstOrNull { it.condition == condition }?.satisfied ?: true

    private fun GeoCoordinate.isValid(): Boolean =
        latitude.isFinite() && latitude in -90.0..90.0 &&
            longitude.isFinite() && longitude in -180.0..180.0
}
