package com.gadarts.returnfire.systems.character.react

import com.badlogic.gdx.ai.msg.Telegram
import com.gadarts.returnfire.Services
import com.gadarts.returnfire.components.ComponentsMapper
import com.gadarts.returnfire.systems.GameSessionData
import com.gadarts.returnfire.systems.HandlerOnEvent
import com.gadarts.returnfire.systems.character.CharacterSystem

class CharacterSystemOnPlayerWeaponShotSecondary(private val characterSystem: CharacterSystem) :
    HandlerOnEvent {

    override fun react(msg: Telegram, gameSessionData: GameSessionData, services: Services) {
        val arm = ComponentsMapper.secondaryArm.get(gameSessionData.player)
        val relativePosition = arm.relativePos
        characterSystem.positionSpark(
            arm,
            ComponentsMapper.modelInstance.get(gameSessionData.player).gameModelInstance.modelInstance,
            relativePosition
        )
        val armProperties = arm.armProperties
        characterSystem.createBullet(
            armProperties.speed,
            relativePosition
        )
        services.soundPlayer.playPositionalSound(
            armProperties.shootingSound,
            randomPitch = false,
            gameSessionData.player,
            gameSessionData.camera
        )
    }


}
