package com.gadarts.returnfire.systems

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.Family
import com.badlogic.ashley.utils.ImmutableArray
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.math.Quaternion
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.utils.TimeUtils
import com.gadarts.returnfire.assets.definitions.SoundDefinition
import com.gadarts.returnfire.components.ComponentsMapper
import com.gadarts.returnfire.components.TurretComponent
import com.gadarts.returnfire.systems.events.SystemEvents
import com.gadarts.returnfire.systems.events.data.CharacterWeaponShotEventData
import kotlin.math.abs
import kotlin.math.min


class EnemySystem : GameEntitySystem() {
    private val cannonSound by lazy { managers.assetsManager.getAssetByDefinition(SoundDefinition.CANNON) }
    override val subscribedEvents: Map<SystemEvents, HandlerOnEvent> = mapOf()

    private val turretEntities: ImmutableArray<Entity> by lazy {
        engine.getEntitiesFor(Family.all(TurretComponent::class.java).get())
    }


    override fun update(deltaTime: Float) {
        for (turret in turretEntities) {
            if (ComponentsMapper.character.get(ComponentsMapper.turret.get(turret).base).dead) continue

            attack(deltaTime, turret)
        }
    }

    private fun attack(
        deltaTime: Float,
        enemy: Entity
    ) {
        val modelInstanceComponent = ComponentsMapper.modelInstance.get(enemy)
        val transform =
            modelInstanceComponent.gameModelInstance.modelInstance.transform
        val position = transform.getTranslation(auxVector2)
        val player = gameSessionData.player
        val playerPosition =
            ComponentsMapper.modelInstance.get(player).gameModelInstance.modelInstance.transform.getTranslation(
                auxVector1
            )
        if (position.dst2(playerPosition) > 90F) return

        val directionToPlayer = auxVector3.set(playerPosition).sub(position).nor()
        val currentRotation = transform.getRotation(auxQuat)

        val forwardDirection =
            auxVector4.set(1f, 0f, 0f).rot(auxMatrix.idt().rotate(currentRotation))
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
            val enemyComponent = ComponentsMapper.enemy.get(enemy)
            val now = TimeUtils.millis()
            if (enemyComponent.attackReady) {
                enemyComponent.attackReady = false
                enemyComponent.attackReadyTime = now + 3000L
                managers.soundPlayer.play(cannonSound)
                CharacterWeaponShotEventData.set(
                    enemy,
                    auxMatrix.idt().set(
                        auxQuat.setFromCross(
                            Vector3.X,
                            ComponentsMapper.modelInstance.get(player).gameModelInstance.modelInstance.transform.getTranslation(
                                auxVector1
                            ).sub(position).nor()
                        )
                    ),
                )
                managers.dispatcher.dispatchMessage(SystemEvents.CHARACTER_WEAPON_ENGAGED_PRIMARY.ordinal)
            } else if (enemyComponent.attackReadyTime <= now) {
                enemyComponent.attackReady = true
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
        private val auxMatrix = Matrix4()
        private const val ROTATION_STEP_SIZE = 40F
    }
}
