package com.gadarts.returnfire.systems

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.Family
import com.badlogic.ashley.utils.ImmutableArray
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.math.Quaternion
import com.badlogic.gdx.math.Vector3
import com.gadarts.returnfire.components.ComponentsMapper
import com.gadarts.returnfire.components.EnemyComponent
import com.gadarts.returnfire.systems.events.SystemEvents
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.sign


class EnemySystem : GameEntitySystem() {

    private val enemyEntities: ImmutableArray<Entity> by lazy {
        engine.getEntitiesFor(Family.all(EnemyComponent::class.java).get())
    }

    override val subscribedEvents: Map<SystemEvents, HandlerOnEvent> = emptyMap()

    override fun update(deltaTime: Float) {
        for (enemy in enemyEntities) {
            val transform = ComponentsMapper.modelInstance.get(enemy).gameModelInstance.modelInstance.transform
            val position = transform.getTranslation(auxVector2)
            val playerPosition =
                ComponentsMapper.modelInstance.get(gameSessionData.player).gameModelInstance.modelInstance.transform.getTranslation(
                    auxVector1
                )
            val directionToPlayer = auxVector3.set(playerPosition).sub(position).nor()
            val currentRotation = transform.getRotation(auxQuat)

            val forwardDirection = auxVector4.set(1f, 0f, 0f).rot(auxMatrix.idt().rotate(currentRotation))
            val forwardXZ = auxVector5.set(forwardDirection.x, 0f, forwardDirection.z).nor()
            val playerDirectionXZ = auxVector6.set(directionToPlayer.x, 0f, directionToPlayer.z).nor()
            val angle = MathUtils.acos(forwardXZ.dot(playerDirectionXZ)) * MathUtils.radiansToDegrees

            val crossY = forwardXZ.crs(playerDirectionXZ)
            val rotationDirection = if (crossY.y > 0) {
                1F
            } else {
                -1F
            }
            val effectiveRotation = min(ROTATION_STEP_SIZE * deltaTime, angle) * rotationDirection

            if (abs(angle - abs(effectiveRotation)) > 0.1f) {
                transform.setFromEulerAngles(
                    currentRotation.yaw + effectiveRotation,
                    currentRotation.pitch,
                    currentRotation.roll
                ).trn(position)
            } else {
                transform.getRotation(currentRotation)
                val angleDiffAroundZ = MathUtils.acos(
                    auxVector4.set(Vector3.X).rot(auxMatrix.set(currentRotation)).dot(directionToPlayer)
                ) * MathUtils.radiansToDegrees
                val cross = auxVector5.set(Vector3.X).rot(auxMatrix).crs(directionToPlayer)
                val sign = sign(cross.z) * -1F

                val angleAroundZ = currentRotation.getAngleAround(Vector3.Z)
                if (angleDiffAroundZ > 2F) {
                    if (sign > 0 && (angleAroundZ < 45F || angleAroundZ > 270F)) {
                        transform.rotate(Vector3.Z, sign)
                    } else if (sign < 0 && currentRotation.roll > -25F) {
                        transform.rotate(Vector3.Z, sign)
                    }
                }
            }
        }
    }

    override fun resume(delta: Long) {
    }

    override fun dispose() {
    }

    companion object {
        private val auxVector1 = Vector3()
        private val auxVector2 = Vector3()
        private val auxVector3 = Vector3()
        private val auxVector4 = Vector3()
        private val auxVector5 = Vector3()
        private val auxVector6 = Vector3()
        private val auxQuat = Quaternion()
        private val auxQuat2 = Quaternion()
        private val auxMatrix = Matrix4()
        private val auxMatrix2 = Matrix4()
        private const val ROTATION_STEP_SIZE = 40F
    }
}
