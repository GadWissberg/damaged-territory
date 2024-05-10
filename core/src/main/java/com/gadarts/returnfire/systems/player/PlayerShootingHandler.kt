package com.gadarts.returnfire.systems.player

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.utils.TimeUtils
import com.gadarts.returnfire.assets.GameAssetManager
import com.gadarts.returnfire.assets.ModelsDefinitions
import com.gadarts.returnfire.components.ArmComponent
import com.gadarts.returnfire.components.ComponentsMapper

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

    fun update(player: Entity, subscribers: HashSet<PlayerSystemEventsSubscriber>) {
        var armComp: ArmComponent = ComponentsMapper.primaryArm.get(player)
        handleShooting(priShooting, armComp, priBulletsPool, player, subscribers)
        armComp = ComponentsMapper.secondaryArm.get(player)
        handleShooting(secShooting, armComp, secBulletsPool, player, subscribers)
    }

    private fun handleShooting(
        shooting: Boolean,
        armComp: ArmComponent,
        pool: BulletsPool,
        player: Entity,
        subscribers: HashSet<PlayerSystemEventsSubscriber>
    ) {
        if (!shooting) return
        val now = TimeUtils.millis()
        if (armComp.loaded <= now) {
            armComp.calculateRelativePosition(player)
            armComp.displaySpark = now
            armComp.loaded = now + armComp.armProperties.reloadDuration
            subscribers.forEach {
                it.onPlayerWeaponShot(
                    player,
                    pool.obtain(),
                    armComp
                )
            }
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
