package com.gadarts.returnfire.systems.character.react

import com.badlogic.gdx.ai.msg.Telegram
import com.gadarts.returnfire.components.ComponentsMapper
import com.gadarts.returnfire.managers.GamePlayManagers
import com.gadarts.returnfire.systems.HandlerOnEvent
import com.gadarts.returnfire.systems.character.CharacterSystem
import com.gadarts.returnfire.systems.data.GameSessionData
import com.gadarts.returnfire.systems.events.data.CharacterWeaponShotEventData

class CharacterSystemOnCharacterWeaponShotSecondary(characterSystem: CharacterSystem) :
    CharacterSystemOnCharacterWeaponShot(characterSystem),
    HandlerOnEvent {
    override fun react(msg: Telegram, gameSessionData: GameSessionData, gamePlayManagers: GamePlayManagers) {
        val shooter = CharacterWeaponShotEventData.shooter
        shoot(
            gameSessionData,
            gamePlayManagers,
            ComponentsMapper.secondaryArm.get(CharacterWeaponShotEventData.shooter),
            shooter
        )
    }


}
