package com.gadarts.returnfire.systems.player

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.ai.msg.MessageDispatcher
import com.badlogic.gdx.graphics.PerspectiveCamera
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.math.Quaternion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.utils.TimeUtils
import com.gadarts.returnfire.SoundPlayer
import com.gadarts.returnfire.assets.GameAssetManager
import com.gadarts.returnfire.components.ComponentsMapper
import com.gadarts.returnfire.components.ComponentsMapper.player
import com.gadarts.returnfire.model.GameMap
import com.gadarts.returnfire.systems.GameSessionData.Companion.REGION_SIZE
import com.gadarts.returnfire.systems.map.MapUtils
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min


class PlayerMovementHandler {
    private lateinit var assetsManager: GameAssetManager
    private lateinit var camera: PerspectiveCamera
    private var tiltAnimationHandler = TiltAnimationHandler()
    private var rotToAdd = 0F
    private var desiredDirectionChanged: Boolean = false
    private val desiredVelocity = Vector2()
    private val prevPos = Vector3()

    fun onTouchUp() {
        desiredVelocity.setZero()
    }

    private fun handleRotation(deltaTime: Float, player: Entity) {
        if (desiredDirectionChanged && !desiredVelocity.isZero) {
            calculateRotation(deltaTime, player)
        } else {
            tiltAnimationHandler.lowerRotationTilt()
        }
        applyRotation(player)
    }

    private fun handleAcceleration(player: Entity) {
        val currentVelocity =
            ComponentsMapper.player.get(player).getCurrentVelocity(auxVector2_1)
        if (desiredVelocity.len2() > 0.5F) {
            currentVelocity.setLength2(
                min(
                    currentVelocity.len2() + (ACCELERATION),
                    MAX_SPEED
                )
            )
            tiltAnimationHandler.onAcceleration()
        } else {
            currentVelocity.setLength2(
                max(
                    currentVelocity.len2() - (DECELERATION),
                    1F
                )
            )
            tiltAnimationHandler.onDeceleration()
        }
        ComponentsMapper.player.get(player).setCurrentVelocity(currentVelocity)
    }

    private fun calculateRotation(deltaTime: Float, player: Entity) {
        val rotBefore = rotToAdd
        updateRotationStep(player)
        val currentVelocity =
            ComponentsMapper.player.get(player).getCurrentVelocity(auxVector2_1)
        val diff = abs(currentVelocity.angleDeg() - desiredVelocity.angleDeg())
        if ((rotBefore < 0 && rotToAdd < 0) || (rotBefore > 0 && rotToAdd > 0) && diff > ROT_EPSILON) {
            rotate(currentVelocity, deltaTime, player)
        } else {
            desiredDirectionChanged = false
        }
    }

    private fun rotate(currentVelocity: Vector2, deltaTime: Float, player: Entity) {
        currentVelocity.rotateDeg(rotToAdd * deltaTime)
        ComponentsMapper.player.get(player).setCurrentVelocity(currentVelocity)
        tiltAnimationHandler.onRotation(rotToAdd)
    }

    private fun updateRotationStep(player: Entity) {
        if (desiredVelocity.isZero) return
        val playerComponent = ComponentsMapper.player.get(player)
        val diff =
            desiredVelocity.angleDeg() - playerComponent.getCurrentVelocity(auxVector2_1)
                .angleDeg()
        val negativeRotation = auxVector2_1.set(1F, 0F).setAngleDeg(diff).angleDeg() > 180
        rotToAdd = if (negativeRotation && rotToAdd < 0) {
            max(rotToAdd - ROTATION_INCREASE, -MAX_ROTATION_STEP)
        } else if (!negativeRotation && rotToAdd > 0) {
            min(rotToAdd + ROTATION_INCREASE, MAX_ROTATION_STEP)
        } else {
            INITIAL_ROTATION_STEP * (if (negativeRotation) -1F else 1F)
        }
    }

    private fun activateStrafing(player: Entity) {
        val playerComponent = ComponentsMapper.player.get(player)
        playerComponent.strafing =
            ComponentsMapper.modelInstance.get(player).gameModelInstance.modelInstance.transform.getRotation(
                auxQuat
            ).getAngleAround(
                Vector3.Y
            )
        tiltAnimationHandler.onStrafeActivated()
    }

    fun onTouchPadTouched(deltaX: Float, deltaY: Float, player: Entity) {
        if (deltaX != 0F || deltaY != 0F) {
            updateDesiredDirection(deltaX, deltaY, player)
        } else {
            desiredVelocity.setZero()
        }
    }

    private fun updateDesiredDirection(deltaX: Float, deltaY: Float, player: Entity) {
        desiredVelocity.set(deltaX, deltaY)
        desiredDirectionChanged = true
        updateRotationStep(player)
    }

    fun update(
        player: Entity,
        deltaTime: Float,
        currentMap: GameMap,
        soundPlayer: SoundPlayer,
        dispatcher: MessageDispatcher
    ) {
        handleRotation(deltaTime, player)
        handleAcceleration(player)
        val transform =
            ComponentsMapper.modelInstance.get(player).gameModelInstance.modelInstance.transform
        transform.getTranslation(prevPos)
        applyMovementWithRegionCheck(player, deltaTime, currentMap, dispatcher)
        updateBlastVelocity(player)
        tiltAnimationHandler.update(player)
    }

    private fun updateBlastVelocity(player: Entity) {
        val playerComponent = ComponentsMapper.player.get(player)
        val blastVelocity = playerComponent.getBlastVelocity(auxVector2_1)
        if (blastVelocity.len2() > 0F) {
            blastVelocity.setLength2(max(blastVelocity.len2() - 0.1F, 0F))
            playerComponent.setBlastVelocity(blastVelocity)
        }
    }

    private fun applyMovementWithRegionCheck(
        p: Entity,
        delta: Float,
        currentMap: GameMap,
        dispatcher: MessageDispatcher
    ) {
        val transform =
            ComponentsMapper.modelInstance.get(p).gameModelInstance.modelInstance.transform
        val currentPosition = transform.getTranslation(auxVector3_2)
        val prevColumn = floor(currentPosition.x / REGION_SIZE)
        val prevRow = floor(currentPosition.z / REGION_SIZE)
        applyMovement(delta, p, currentMap, player.get(p).getCurrentVelocity(auxVector2_1))
        applyMovement(delta, p, currentMap, player.get(p).getBlastVelocity(auxVector2_1))
        val newPosition = transform.getTranslation(auxVector3_2)
        MapUtils.notifyEntityRegionChanged(
            newPosition,
            prevRow.toInt(),
            prevColumn.toInt(),
            dispatcher
        )
    }

    fun onTouchDown(lastTouchDown: Long, player: Entity) {
        if (TimeUtils.timeSinceMillis(lastTouchDown) <= STRAFE_PRESS_INTERVAL) {
            activateStrafing(player)
        } else {
            deactivateStrafing(player)
        }
    }

    private fun deactivateStrafing(player: Entity) {
        val playerComponent = ComponentsMapper.player.get(player)
        if (playerComponent.strafing != null) {
            val currentVelocity = playerComponent.getCurrentVelocity(auxVector2_1)
            val newVelocity = currentVelocity.setAngleDeg(playerComponent.strafing!!)
            playerComponent.setCurrentVelocity(newVelocity)
        }
        playerComponent.strafing = null
    }

    private fun applyMovement(
        deltaTime: Float,
        player: Entity,
        currentMap: GameMap,
        velocity: Vector2
    ) {
        if (velocity.len2() > 1F) {
            val step = auxVector3_1.set(velocity.x, 0F, -velocity.y)
            step.setLength2(step.len2() - 1F).scl(deltaTime)
            val transform =
                ComponentsMapper.modelInstance.get(player).gameModelInstance.modelInstance.transform
            transform.trn(step)
            clampPosition(transform, currentMap)
            ComponentsMapper.modelInstance.get(player).gameModelInstance.updateBoundingBoxPosition()
        }
    }

    private fun clampPosition(
        transform: Matrix4,
        currentMap: GameMap,
    ) {
        val newPos = transform.getTranslation(auxVector3_1)
        newPos.x = MathUtils.clamp(newPos.x, 0F, currentMap.tilesMapping.size.toFloat())
        newPos.z = MathUtils.clamp(newPos.z, 0F, currentMap.tilesMapping[0].size.toFloat())
        transform.setTranslation(newPos)
    }

    private fun applyRotation(player: Entity) {
        val transform =
            ComponentsMapper.modelInstance.get(player).gameModelInstance.modelInstance.transform
        val position = transform.getTranslation(auxVector3_1)
        val playerComponent = ComponentsMapper.player.get(player)
        val currentVelocity = playerComponent.getCurrentVelocity(auxVector2_1)
        transform.setToRotation(
            Vector3.Y,
            (if (playerComponent.strafing != null) playerComponent.strafing else currentVelocity.angleDeg())!!
        )
        transform.rotate(Vector3.Z, -IDLE_Z_TILT_DEGREES)
        transform.setTranslation(position)
    }

    fun initialize(assetsManager: GameAssetManager, camera: PerspectiveCamera) {
        this.assetsManager = assetsManager
        this.camera = camera
    }

    companion object {
        private const val MAX_ROTATION_STEP = 200F
        private const val ROTATION_INCREASE = 2F
        private const val INITIAL_ROTATION_STEP = 6F
        private val auxVector2_1 = Vector2()
        private val auxVector3_1 = Vector3()
        private val auxVector3_2 = Vector3()
        private val auxQuat = Quaternion()
        private const val ROT_EPSILON = 0.5F
        private const val MAX_SPEED = 14F
        private const val ACCELERATION = 0.04F
        private const val DECELERATION = 0.06F
        private const val IDLE_Z_TILT_DEGREES = 12F
        private const val STRAFE_PRESS_INTERVAL = 500
    }
}
