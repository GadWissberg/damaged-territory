package com.gadarts.returnfire.systems.map

import com.badlogic.gdx.math.MathUtils.random
import com.badlogic.gdx.utils.TimeUtils
import com.gadarts.returnfire.assets.definitions.SoundDefinition
import com.gadarts.returnfire.managers.GamePlayManagers

class AmbSoundsHandler {
    fun resume(delta: Long) {
        nextAmbSound += delta
    }

    fun update(gamePlayManagers: GamePlayManagers) {
        val now = TimeUtils.millis()
        if (nextAmbSound < now) {
            nextAmbSound = now + random(AMB_SND_INTERVAL_MIN, AMB_SND_INTERVAL_MAX)
            gamePlayManagers.soundPlayer.play(gamePlayManagers.assetsManager.getAssetByDefinition(ambSounds.random()))
        }
    }

    private val ambSounds = listOf(
        SoundDefinition.AMB_EAGLE,
        SoundDefinition.AMB_WIND,
        SoundDefinition.AMB_OUD
    )

    private var nextAmbSound: Long = TimeUtils.millis() + random(
        AMB_SND_INTERVAL_MIN,
        AMB_SND_INTERVAL_MAX
    )

    companion object {
        private const val AMB_SND_INTERVAL_MIN = 7000
        private const val AMB_SND_INTERVAL_MAX = 22000

    }


}
