package com.gadarts.returnfire.systems.character.react

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.math.Quaternion
import com.badlogic.gdx.math.Vector3
import com.gadarts.returnfire.Managers
import com.gadarts.returnfire.components.ArmComponent
import com.gadarts.returnfire.components.ComponentsMapper
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
        managers: Managers,
        arm: ArmComponent,
        shooter: Entity,
    ) {
        val sparkComponent = ComponentsMapper.spark.get(arm.spark)
        val relativePosition = sparkComponent.relativePositionCalculator.calculate(
            sparkComponent.parent,
            auxVector1
        )
        val player = gameSessionData.player
        characterSystem.positionSpark(
            arm,
            ComponentsMapper.modelInstance.get(player).gameModelInstance.modelInstance,
            relativePosition
        )
        initiateTurretKickoff(shooter)
        BulletCreationRequestEventData.set(
            arm,
            ComponentsMapper.player.has(CharacterWeaponShotEventData.shooter),
            relativePosition,
            CharacterWeaponShotEventData.direction
        )
        managers.dispatcher.dispatchMessage(SystemEvents.BULLET_CREATION_REQUEST.ordinal)
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
        private val auxQuat = Quaternion()
    }
}
