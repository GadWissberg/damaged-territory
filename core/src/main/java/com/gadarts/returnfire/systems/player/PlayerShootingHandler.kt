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

    var secondaryCreationSide = false
    private lateinit var priBulletsPool: BulletsPool
    private lateinit var secBulletsPool: BulletsPool
    private var priShooting: Boolean = false
    private var secShooting: Boolean = false

    fun initialize(am: GameAssetManager) {
        priBulletsPool = BulletsPool(am.getAssetByDefinition(ModelsDefinitions.BULLET))
        secBulletsPool = BulletsPool(am.getAssetByDefinition(ModelsDefinitions.MISSILE))

    }

    fun update(player: Entity, dispatcher: MessageDispatcher) {
        var armComp: ArmComponent = ComponentsMapper.primaryArm.get(player)
        handleShooting(priShooting, armComp, priBulletsPool, player, dispatcher)
        armComp = ComponentsMapper.secondaryArm.get(player)
        handleShooting(secShooting, armComp, secBulletsPool, player, dispatcher)
    }

    private fun handleShooting(
        shooting: Boolean,
        armComp: ArmComponent,
        pool: BulletsPool,
        player: Entity,
        dispatcher: MessageDispatcher
    ) {
        if (!shooting) return
        val now = TimeUtils.millis()
        if (armComp.loaded <= now) {
            armComp.calculateRelativePosition(player)
            armComp.displaySpark = now
            armComp.loaded = now + armComp.armProperties.reloadDuration
            dispatcher.dispatchMessage(SystemEvents.PLAYER_WEAPON_SHOT.ordinal, pool.obtain())
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
