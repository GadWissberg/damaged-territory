package com.gadarts.returnfire.systems.character.react

import com.badlogic.gdx.ai.msg.Telegram
import com.gadarts.returnfire.Managers
import com.gadarts.returnfire.components.ComponentsMapper
import com.gadarts.returnfire.systems.HandlerOnEvent
import com.gadarts.returnfire.systems.character.CharacterSystem
import com.gadarts.returnfire.systems.data.GameSessionData
import com.gadarts.returnfire.systems.events.data.CharacterWeaponShotEventData

class CharacterSystemOnCharacterWeaponShotPrimary(characterSystem: CharacterSystem) :
    CharacterSystemOnCharacterWeaponShot(characterSystem),
    HandlerOnEvent {
    override fun react(msg: Telegram, gameSessionData: GameSessionData, managers: Managers) {
        val shooter = CharacterWeaponShotEventData.shooter
        shoot(gameSessionData, managers, ComponentsMapper.primaryArm.get(shooter))
    }


}
