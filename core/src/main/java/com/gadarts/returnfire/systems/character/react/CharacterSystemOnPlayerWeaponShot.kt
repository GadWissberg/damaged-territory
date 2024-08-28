package com.gadarts.returnfire.systems.character.react

import com.gadarts.returnfire.Managers
import com.gadarts.returnfire.components.ArmComponent
import com.gadarts.returnfire.components.ComponentsMapper
import com.gadarts.returnfire.systems.HandlerOnEvent
import com.gadarts.returnfire.systems.character.CharacterSystem
import com.gadarts.returnfire.systems.data.GameSessionData

abstract class CharacterSystemOnPlayerWeaponShot(private val characterSystem: CharacterSystem) :
    HandlerOnEvent {

    protected fun shoot(
        gameSessionData: GameSessionData,
        managers: Managers,
        arm: ArmComponent
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
        val armProperties = arm.armProperties
        characterSystem.createBullet(
            armProperties,
            relativePosition,
            arm.spark
        )
        managers.soundPlayer.playPositionalSound(
            armProperties.shootingSound,
            randomPitch = false,
            gameSessionData.player,
            gameSessionData.renderData.camera
        )
    }

}
