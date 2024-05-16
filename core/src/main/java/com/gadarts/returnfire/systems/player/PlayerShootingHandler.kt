package com.gadarts.returnfire.systems.player

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.ai.msg.MessageDispatcher
import com.badlogic.gdx.utils.TimeUtils
import com.gadarts.returnfire.assets.GameAssetManager
import com.gadarts.returnfire.assets.ModelsDefinitions
import com.gadarts.returnfire.components.ArmComponent
import com.gadarts.returnfire.components.ComponentsMapper
import com.gadarts.returnfire.systems.SystemEvents

class PlayerShootingHandler {

    private lateinit var dispatcher: MessageDispatcher
    var secondaryCreationSide = false
    private lateinit var priBulletsPool: BulletsPool
    private lateinit var secBulletsPool: BulletsPool
    private var priShooting: Boolean = false
    private var secShooting: Boolean = false

    fun initialize(am: GameAssetManager, dispatcher: MessageDispatcher) {
        priBulletsPool = BulletsPool(am.getAssetByDefinition(ModelsDefinitions.BULLET))
        secBulletsPool = BulletsPool(am.getAssetByDefinition(ModelsDefinitions.MISSILE))
        this.dispatcher = dispatcher
    }

    fun update(player: Entity) {
        var armComp: ArmComponent = ComponentsMapper.primaryArm.get(player)
        handleShooting(priShooting, armComp, priBulletsPool, player, SystemEvents.PLAYER_WEAPON_SHOT_PRIMARY)
        armComp = ComponentsMapper.secondaryArm.get(player)
        handleShooting(secShooting, armComp, secBulletsPool, player, SystemEvents.PLAYER_WEAPON_SHOT_SECONDARY)
    }

    private fun handleShooting(
        shooting: Boolean,
        armComp: ArmComponent,
        pool: BulletsPool,
        player: Entity,
        event: SystemEvents,
    ) {
        if (!shooting) return
        val now = TimeUtils.millis()
        if (armComp.loaded <= now) {
            armComp.calculateRelativePosition(player)
            armComp.displaySpark = now
            armComp.loaded = now + armComp.armProperties.reloadDuration
            dispatcher.dispatchMessage(event.ordinal, pool.obtain())
        }
    }

    fun onPrimaryWeaponButtonPressed() {
        priShooting = true
    }

    fun onPrimaryWeaponButtonReleased() {
        priShooting = false
    }

    fun onSecondaryWeaponButtonPressed() {
        secShooting = true
    }

    fun onSecondaryWeaponButtonReleased() {
        secShooting = false
    }

}
