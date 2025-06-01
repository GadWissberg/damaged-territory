package com.gadarts.returnfire.systems.player.handlers

import com.badlogic.gdx.math.Vector2
import com.gadarts.returnfire.managers.SoundManager
import com.gadarts.returnfire.systems.EntityBuilder
import com.gadarts.returnfire.systems.character.CharacterShootingHandler
import com.gadarts.returnfire.systems.events.SystemEvents
import com.gadarts.shared.GameAssetManager
import com.gadarts.shared.assets.definitions.SoundDefinition
import kotlin.math.abs

class PlayerShootingHandler(entityBuilder: EntityBuilder, soundManager: SoundManager, assetsManager: GameAssetManager) :
    CharacterShootingHandler(
        entityBuilder,
        soundManager,
        assetsManager.getAssetByDefinition(
            SoundDefinition.EMPTY
        )
    ) {


    fun onTurretTouchPadTouchDown(deltaX: Float, deltaY: Float) {
        val dst = auxVector2.set(abs(deltaX), abs(deltaY)).dst2(Vector2.Zero)
        priShooting = dst >= 0.9
    }

    fun onTurretTouchPadTouchUp() {
        priShooting = false
    }

    fun toggleSkyAim() {
        aimSky = !aimSky
        dispatcher.dispatchMessage(SystemEvents.PLAYER_AIM_SKY.ordinal, aimSky)
    }

    companion object {
        private val auxVector2 = Vector2()
    }
}
