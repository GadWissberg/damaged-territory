package com.gadarts.returnfire.systems.character.react

import com.badlogic.gdx.ai.msg.Telegram
import com.gadarts.returnfire.Managers
import com.gadarts.returnfire.components.ComponentsMapper
import com.gadarts.returnfire.systems.HandlerOnEvent
import com.gadarts.returnfire.systems.character.CharacterSystem
import com.gadarts.returnfire.systems.data.GameSessionData

class CharacterSystemOnPlayerWeaponShotPrimary(private val characterSystem: CharacterSystem) :
    HandlerOnEvent {

    override fun react(msg: Telegram, gameSessionData: GameSessionData, managers: Managers) {
        val arm = ComponentsMapper.primaryArm.get(gameSessionData.player)
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
            gameSessionData.gameSessionDataRender.camera
        )

    }

}
