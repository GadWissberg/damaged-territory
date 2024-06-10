package com.gadarts.returnfire.systems.player.movement

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.ai.msg.MessageDispatcher
import com.badlogic.gdx.graphics.PerspectiveCamera
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.math.Vector3
import com.gadarts.returnfire.components.ComponentsMapper
import com.gadarts.returnfire.model.GameMap
import com.gadarts.returnfire.systems.GameSessionData
import com.gadarts.returnfire.systems.map.MapUtils
import com.gadarts.returnfire.systems.player.TiltAnimationHandler
import kotlin.math.floor

abstract class PlayerMovementHandler(protected val desiredVelocitySizeThreshold: Float) {
    protected val thrustVelocity: Vector2 = Vector2(desiredVelocitySizeThreshold, 0F)
    protected var tiltAnimationHandler = TiltAnimationHandler()

    private val prevPos = Vector3()

    private fun clampPosition(
        transform: Matrix4,
        currentMap: GameMap,
    ) {
        val newPos = transform.getTranslation(auxVector3_1)
        newPos.x = MathUtils.clamp(newPos.x, 0F, currentMap.tilesMapping.size.toFloat())
        newPos.z = MathUtils.clamp(newPos.z, 0F, currentMap.tilesMapping[0].size.toFloat())
        transform.setTranslation(newPos)
    }

    protected fun takeStep(
        player: Entity,
        deltaTime: Float,
        currentMap: GameMap,
        dispatcher: MessageDispatcher,
        velocity: Vector2
    ) {
        val transform =
            ComponentsMapper.modelInstance.get(player).gameModelInstance.modelInstance.transform
        transform.getTranslation(prevPos)
        applyMovementWithRegionCheck(
            player,
            deltaTime,
            currentMap,
            dispatcher,
            velocity
        )
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
        }
    }

    private fun applyMovementWithRegionCheck(
        p: Entity,
        delta: Float,
        currentMap: GameMap,
        dispatcher: MessageDispatcher,
        velocity: Vector2
    ) {
        val transform =
            ComponentsMapper.modelInstance.get(p).gameModelInstance.modelInstance.transform
        val currentPosition = transform.getTranslation(auxVector3_2)
        val prevColumn = floor(currentPosition.x / GameSessionData.REGION_SIZE)
        val prevRow = floor(currentPosition.z / GameSessionData.REGION_SIZE)
        applyMovement(
            delta, p, currentMap, velocity
        )
        val newPosition = transform.getTranslation(auxVector3_2)
        MapUtils.notifyEntityRegionChanged(
            newPosition,
            prevRow.toInt(),
            prevColumn.toInt(),
            dispatcher
        )
    }

    abstract fun handleAcceleration(player: Entity, maxSpeed: Float, desiredVelocity: Vector2)


    abstract fun onTouchUp(player: Entity, keycode: Int = -1)

    abstract fun initialize(camera: PerspectiveCamera)
    abstract fun thrust(
        player: Entity,
        directionX: Float = 0F,
        directionY: Float = 0F,
        reverse: Boolean = false
    )

    abstract fun update(
        player: Entity,
        deltaTime: Float,
        currentMap: GameMap,
        dispatcher: MessageDispatcher
    )

    abstract fun rotate(player: Entity, clockwise: Int)

    companion object {
        private val auxVector3_1 = Vector3()
        private val auxVector3_2 = Vector3()
        const val MAX_THRUST = 7F
        const val ACCELERATION = 0.04F
        const val DECELERATION = 0.06F
        const val IDLE_Z_TILT_DEGREES = 12F
        const val MAX_ROTATION_STEP = 200F
        const val ROTATION_INCREASE = 2F
    }
}
