package com.gadarts.returnfire.systems.character.react

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.math.Quaternion
import com.badlogic.gdx.math.Vector3
import com.gadarts.returnfire.components.ComponentsMapper
import com.gadarts.returnfire.components.arm.ArmComponent
import com.gadarts.returnfire.managers.GamePlayManagers
import com.gadarts.returnfire.systems.HandlerOnEvent
import com.gadarts.returnfire.systems.character.CharacterSystem
import com.gadarts.returnfire.systems.data.GameSessionData
import com.gadarts.returnfire.systems.events.SystemEvents
import com.gadarts.returnfire.systems.events.data.BulletCreationRequestEventData
import com.gadarts.returnfire.systems.events.data.CharacterWeaponShotEventData

abstract class CharacterSystemOnCharacterWeaponShot(private val characterSystem: CharacterSystem) :
    HandlerOnEvent {

    protected fun shoot(
        gameSessionData: GameSessionData,
        gamePlayManagers: GamePlayManagers,
        arm: ArmComponent,
        shooter: Entity,
    ) {
        val sparkComponent = ComponentsMapper.spark.get(arm.spark)
        val relativePosition = sparkComponent.relativePositionCalculator.calculate(
            sparkComponent.parent,
            auxVector1
        )
        val player = gameSessionData.gamePlayData.player
        characterSystem.positionSpark(
            arm,
            ComponentsMapper.modelInstance.get(player).gameModelInstance.modelInstance,
            relativePosition
        )
        initiateTurretKickoff(shooter)
        val bulletDirection: Matrix4 = calculateBulletDirection(shooter, relativePosition)
        BulletCreationRequestEventData.set(
            arm,
            ComponentsMapper.player.has(CharacterWeaponShotEventData.shooter),
            relativePosition,
            bulletDirection,
            CharacterWeaponShotEventData.target
        )
        gamePlayManagers.dispatcher.dispatchMessage(SystemEvents.BULLET_CREATION_REQUEST.ordinal)
    }

    private fun calculateBulletDirection(
        shooter: Entity,
        relativePosition: Vector3,
    ): Matrix4 {
        val bulletDirection: Matrix4 = if (CharacterWeaponShotEventData.target != null) {
            val targetGameModelInstance =
                ComponentsMapper.modelInstance.get(CharacterWeaponShotEventData.target).gameModelInstance
            val targetTransform =
                targetGameModelInstance.modelInstance.transform
            val shooterPosition =
                ComponentsMapper.modelInstance.get(shooter).gameModelInstance.modelInstance.transform.getTranslation(
                    auxVector4
                )
            val directionToTarget =
                targetTransform.getTranslation(
                    auxVector3
                ).add(targetGameModelInstance.definition?.centerOfMass)
                    .sub(shooterPosition.add(relativePosition)).nor()
            auxMatrix.set(
                auxQuat.setFromCross(
                    Vector3.X,
                    directionToTarget
                )
            )
        } else {
            CharacterWeaponShotEventData.direction
        }
        return bulletDirection
    }

    private fun initiateTurretKickoff(shooter: Entity) {
        if (ComponentsMapper.turretBase.has(shooter)) {
            val turret = ComponentsMapper.turretBase.get(shooter).turret
            val turretComponent = ComponentsMapper.turret.get(turret)
            turretComponent.baseOffsetApplied = true
            turretComponent.setBaseOffset(
                auxVector2.set(Vector3.X).scl(-1F).scl(0.05F).rotate(
                    Vector3.Y,
                    ComponentsMapper.modelInstance.get(turret).gameModelInstance.modelInstance.transform.getRotation(
                        auxQuat
                    ).yaw
                )
            )
        }
    }

    companion object {
        private val auxVector1 = Vector3()
        private val auxVector2 = Vector3()
        private val auxVector3 = Vector3()
        private val auxVector4 = Vector3()
        private val auxQuat = Quaternion()
        private val auxMatrix = Matrix4()
    }
}
