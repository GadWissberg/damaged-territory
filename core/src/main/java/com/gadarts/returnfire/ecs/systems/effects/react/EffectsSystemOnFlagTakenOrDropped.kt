package com.gadarts.returnfire.ecs.systems.effects.react

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.ai.msg.Telegram
import com.gadarts.returnfire.ecs.components.ComponentsMapper
import com.gadarts.returnfire.ecs.systems.HandlerOnEvent
import com.gadarts.returnfire.ecs.systems.data.GameSessionData
import com.gadarts.returnfire.managers.GamePlayManagers

class EffectsSystemOnFlagTakenOrDropped(private val visibility: Boolean) : HandlerOnEvent {
    override fun react(msg: Telegram, gameSessionData: GameSessionData, gamePlayManagers: GamePlayManagers) {
        val entity = msg.extraInfo as Entity
        val haloEffect = ComponentsMapper.modelInstance.get(entity).haloEffect
        if (haloEffect != null) {
            haloEffect.visible = visibility
        }
    }
}