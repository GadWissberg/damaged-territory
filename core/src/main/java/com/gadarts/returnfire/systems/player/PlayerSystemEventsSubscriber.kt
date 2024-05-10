package com.gadarts.returnfire.systems.player

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.graphics.g3d.ModelInstance
import com.gadarts.returnfire.components.ArmComponent
import com.gadarts.returnfire.systems.SystemEventsSubscriber

interface PlayerSystemEventsSubscriber : SystemEventsSubscriber {
    fun onPlayerWeaponShot(
        player: Entity,
        bulletModelInstance: ModelInstance,
        arm: ArmComponent,
    )

    fun onPlayerEnteredNewRegion(player: Entity)

}
