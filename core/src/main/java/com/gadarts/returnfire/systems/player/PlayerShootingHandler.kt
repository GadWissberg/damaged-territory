package com.gadarts.returnfire.systems.player

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.ai.msg.MessageDispatcher
import com.badlogic.gdx.utils.TimeUtils
import com.gadarts.returnfire.components.ArmComponent
import com.gadarts.returnfire.components.ComponentsMapper
import com.gadarts.returnfire.components.bullet.BulletBehavior
import com.gadarts.returnfire.systems.events.SystemEvents
import com.gadarts.returnfire.systems.events.data.PlayerWeaponShotEventData

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
            SystemEvents.PLAYER_WEAPON_SHOT_PRIMARY,
            BulletBehavior.REGULAR,
        )
        armComp = ComponentsMapper.secondaryArm.get(player)
        handleShooting(
            secShooting,
            armComp,
            SystemEvents.PLAYER_WEAPON_SHOT_SECONDARY,
            BulletBehavior.CURVE
        )
    }

    private fun handleShooting(
        shooting: Boolean,
        armComp: ArmComponent,
        event: SystemEvents,
        bulletBehavior: BulletBehavior,
    ) {
        if (!shooting) return
        val now = TimeUtils.millis()
        if (armComp.loaded <= now) {
            armComp.displaySpark = now
            armComp.loaded = now + armComp.armProperties.reloadDuration
            PlayerWeaponShotEventData.set(bulletBehavior)
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
