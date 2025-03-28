package com.gadarts.returnfire.systems.ai.logic

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.math.Quaternion
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.utils.TimeUtils
import com.gadarts.returnfire.components.ComponentsMapper
import com.gadarts.returnfire.managers.GamePlayManagers
import com.gadarts.returnfire.systems.data.GameSessionData
import com.gadarts.returnfire.systems.events.SystemEvents
import com.gadarts.returnfire.systems.events.data.CharacterWeaponShotEventData
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.sqrt

class TurretLogic(private val gameSessionData: GameSessionData, private val gamePlayManagers: GamePlayManagers) {

    fun attack(
        deltaTime: Float,
        enemy: Entity
    ) {
        val modelInstanceComponent = ComponentsMapper.modelInstance.get(enemy)
        val transform =
            modelInstanceComponent.gameModelInstance.modelInstance.transform
        val position = transform.getTranslation(auxVector3_2)
        val player = gameSessionData.gamePlayData.player
        val playerPosition =
            ComponentsMapper.modelInstance.get(player).gameModelInstance.modelInstance.transform.getTranslation(
                auxVector3_1
            )
        if (position.dst2(playerPosition) > 90F) return

        val directionToPlayer = auxVector3_3.set(playerPosition).sub(position).nor()
        val currentRotation = transform.getRotation(auxQuat1)

        val forwardDirection =
            auxVector3_4.set(1f, 0f, 0f).rot(auxMatrix.idt().rotate(currentRotation))
        val forwardXZ = auxVector3_5.set(forwardDirection.x, 0f, forwardDirection.z).nor()
        val playerDirectionXZ = auxVector3_6.set(directionToPlayer.x, 0f, directionToPlayer.z).nor()
        val angle = MathUtils.acos(forwardXZ.dot(playerDirectionXZ)) * MathUtils.radiansToDegrees

        val crossY = forwardXZ.crs(playerDirectionXZ)
        val rotationDirection = if (crossY.y > 0) {
            1F
        } else {
            -1F
        }
        val effectiveRotation = min(ROTATION_STEP_SIZE * deltaTime, angle) * rotationDirection

        if (abs(angle - abs(effectiveRotation)) > 6f) {
            transform.setFromEulerAngles(
                currentRotation.yaw + effectiveRotation,
                currentRotation.pitch,
                currentRotation.roll
            ).trn(position)
        } else {
            val roll = MathUtils.atan2(
                directionToPlayer.y,
                sqrt((directionToPlayer.x * directionToPlayer.x + directionToPlayer.z * directionToPlayer.z))
            )
            auxQuat2.idt().setEulerAngles(currentRotation.yaw, currentRotation.pitch, roll * MathUtils.radiansToDegrees)
            if (MathUtils.isEqual(
                    currentRotation.roll,
                    auxQuat2.roll,
                    6F
                )
            ) {
                val enemyComponent = ComponentsMapper.ai.get(enemy)
                val now = TimeUtils.millis()
                if (enemyComponent.attackReady) {
                    enemyComponent.attackReady = false
                    val armProperties = ComponentsMapper.primaryArm.get(enemy).armProperties
                    enemyComponent.attackReadyTime =
                        now + armProperties.reloadDuration
                    gamePlayManagers.soundPlayer.play(armProperties.shootingSound, position)
                    CharacterWeaponShotEventData.setWithTarget(
                        enemy,
                        player!!,
                    )
                    gamePlayManagers.dispatcher.dispatchMessage(SystemEvents.CHARACTER_WEAPON_ENGAGED_PRIMARY.ordinal)
                } else if (enemyComponent.attackReadyTime <= now) {
                    enemyComponent.attackReady = true
                }
            } else {
                currentRotation.slerp(auxQuat2, 1F * deltaTime)
                transform.getTranslation(auxVector3_1)
                transform.idt().set(currentRotation).trn(position)
            }
        }
    }

    companion object {
        private val auxVector3_1 = Vector3()
        private val auxVector3_2 = Vector3()
        private val auxVector3_3 = Vector3()
        private val auxVector3_4 = Vector3()
        private val auxVector3_5 = Vector3()
        private val auxVector3_6 = Vector3()
        private val auxMatrix = Matrix4()
        private val auxQuat1 = Quaternion()
        private val auxQuat2 = Quaternion()
        private const val ROTATION_STEP_SIZE = 40F
    }
}
