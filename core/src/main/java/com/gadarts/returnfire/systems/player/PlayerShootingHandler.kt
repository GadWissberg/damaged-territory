package com.gadarts.returnfire.systems.player

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.ai.msg.MessageDispatcher
import com.badlogic.gdx.utils.TimeUtils
import com.gadarts.returnfire.components.ArmComponent
import com.gadarts.returnfire.components.ComponentsMapper
import com.gadarts.returnfire.systems.events.SystemEvents
import com.gadarts.returnfire.systems.events.data.CharacterWeaponShotEventData

class PlayerShootingHandler {
    private lateinit var player: Entity
    private lateinit var dispatcher: MessageDispatcher
    var secondaryCreationSide = false
    private var priShooting: Boolean = false
    private var secShooting: Boolean = false

    fun initialize(
        dispatcher: MessageDispatcher,
        player: Entity
    ) {

        this.player = player
        this.dispatcher = dispatcher
    }

    fun update() {
        var armComp: ArmComponent = ComponentsMapper.primaryArm.get(player)
        handleShooting(
            priShooting,
            armComp,
            SystemEvents.CHARACTER_WEAPON_ENGAGED_PRIMARY,
        )
        armComp = ComponentsMapper.secondaryArm.get(player)
        handleShooting(
            secShooting,
            armComp,
            SystemEvents.CHARACTER_WEAPON_ENGAGED_SECONDARY,
        )
    }

    private fun handleShooting(
        shooting: Boolean,
        armComp: ArmComponent,
        event: SystemEvents,
    ) {
        if (!shooting) return

        val now = TimeUtils.millis()
        if (armComp.loaded <= now) {
            armComp.displaySpark = now
            armComp.loaded = now + armComp.armProperties.reloadDuration
            CharacterWeaponShotEventData.set(
                player,
                ComponentsMapper.modelInstance.get(player).gameModelInstance.modelInstance.transform
            )
            dispatcher.dispatchMessage(event.ordinal)
        }
    }

    fun startPrimaryShooting() {
        priShooting = true
    }

    fun stopPrimaryShooting() {
        priShooting = false
    }

    fun startSecondaryShooting() {
        secShooting = true
    }

    fun stopSecondaryShooting() {
        secShooting = false
    }

}
