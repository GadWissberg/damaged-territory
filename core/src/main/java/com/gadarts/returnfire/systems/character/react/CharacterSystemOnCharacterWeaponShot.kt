package com.gadarts.returnfire.systems.character.react

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
    ) {
        val sparkComponent = ComponentsMapper.spark.get(arm.spark)
        val relativePosition = sparkComponent.relativePositionCalculator.calculate(
            sparkComponent.parent,
            arm.relativePos
        )
        characterSystem.positionSpark(
            arm,
            ComponentsMapper.modelInstance.get(gameSessionData.player).gameModelInstance.modelInstance,
            relativePosition
        )
        BulletCreationRequestEventData.set(
            arm,
            ComponentsMapper.player.has(CharacterWeaponShotEventData.shooter),
            relativePosition,
            CharacterWeaponShotEventData.direction
        )
        managers.dispatcher.dispatchMessage(SystemEvents.BULLET_CREATION_REQUEST.ordinal)
    }

}
