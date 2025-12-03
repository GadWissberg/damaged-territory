package com.gadarts.returnfire.ecs.systems.ai.logic

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.Family
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.math.Quaternion
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.utils.TimeUtils
import com.gadarts.returnfire.ecs.components.BrownComponent
import com.gadarts.returnfire.ecs.components.ComponentsMapper
import com.gadarts.returnfire.ecs.components.GreenComponent
import com.gadarts.returnfire.ecs.systems.ai.AiUtils
import com.gadarts.returnfire.ecs.systems.events.SystemEvents
import com.gadarts.returnfire.ecs.systems.events.data.CharacterWeaponShotEventData
import com.gadarts.returnfire.managers.GamePlayManagers
import com.gadarts.shared.data.CharacterColor
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.sqrt

class AiGuardTurretLogic(private val gamePlayManagers: GamePlayManagers) {
    private val greens by lazy {
        gamePlayManagers.ecs.engine.getEntitiesFor(
            Family.all(GreenComponent::class.java).get()
        )
    }
    private val browns by lazy {
        gamePlayManagers.ecs.engine.getEntitiesFor(
            Family.all(BrownComponent::class.java).get()
        )
    }


    fun attack(
        deltaTime: Float,
        turret: Entity
    ) {
        if (gamePlayManagers.assetsManager.gameSettings.aiAttackDisabled) return

        val baseAiComponent = ComponentsMapper.baseAi.get(turret)
        val target = baseAiComponent.target
        if (target != null) {
            val modelInstanceComponent = ComponentsMapper.modelInstance.get(turret)
            val transform =
                modelInstanceComponent.gameModelInstance.modelInstance.transform
            val position = transform.getTranslation(auxVector3_2)
            val targetPosition =
                ComponentsMapper.modelInstance.get(target).gameModelInstance.modelInstance.transform.getTranslation(
                    auxVector3_1
                )
            if (position.dst2(targetPosition) > MAX_DISTANCE) return

            val directionToTarget = auxVector3_3.set(targetPosition).sub(position).nor()
            val currentRotation = transform.getRotation(auxQuat1)

            val forwardDirection =
                auxVector3_4.set(1f, 0f, 0f).rot(auxMatrix.idt().rotate(currentRotation))
            val forwardXZ = auxVector3_5.set(forwardDirection.x, 0f, forwardDirection.z).nor()
            val playerDirectionXZ = auxVector3_6.set(directionToTarget.x, 0f, directionToTarget.z).nor()
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
                    directionToTarget.y,
                    sqrt((directionToTarget.x * directionToTarget.x + directionToTarget.z * directionToTarget.z))
                )
                auxQuat2.idt()
                    .setEulerAngles(currentRotation.yaw, currentRotation.pitch, roll * MathUtils.radiansToDegrees)
                if (MathUtils.isEqual(
                        currentRotation.roll,
                        auxQuat2.roll,
                        6F
                    )
                ) {
                    val turretEnemyAiComponent = ComponentsMapper.turretEnemyAi.get(turret)
                    val now = TimeUtils.millis()
                    if (turretEnemyAiComponent.attackReady) {
                        turretEnemyAiComponent.attackReady = false
                        val armProperties = ComponentsMapper.primaryArm.get(turret).armProperties
                        turretEnemyAiComponent.attackReadyTime =
                            now + armProperties.reloadDuration
                        gamePlayManagers.soundManager.play(armProperties.shootingSound, position)
                        CharacterWeaponShotEventData.setWithTarget(
                            turret,
                            target,
                        )
                        gamePlayManagers.dispatcher.dispatchMessage(SystemEvents.CHARACTER_WEAPON_ENGAGED_PRIMARY.ordinal)
                    } else if (turretEnemyAiComponent.attackReadyTime <= now) {
                        turretEnemyAiComponent.attackReady = true
                    }
                } else {
                    currentRotation.slerp(auxQuat2, 1F * deltaTime)
                    transform.getTranslation(auxVector3_1)
                    transform.idt().set(currentRotation).trn(position)
                }
            }
        } else {
            val color = ComponentsMapper.character.get(ComponentsMapper.turret.get(turret).base).color
            baseAiComponent.target =
                AiUtils.findNearestRivalCharacter(turret, if (color == CharacterColor.GREEN) browns else greens, MAX_DISTANCE)
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
        private const val MAX_DISTANCE = 80F
    }
}
