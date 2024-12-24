package com.gadarts.returnfire.managers

import com.badlogic.gdx.audio.Music
import com.badlogic.gdx.audio.Sound
import com.badlogic.gdx.math.MathUtils
import com.gadarts.returnfire.GameDebugSettings
import com.gadarts.returnfire.assets.definitions.SoundDefinition

class SoundPlayer {

    fun loopSound(sound: Sound): Long {
        if (!GameDebugSettings.SFX) return -1
        return sound.loop(1F, 1F, 0F)
    }

    fun play(sound: Sound): Long {
        if (!GameDebugSettings.SFX) return -1
        return sound.play(
            MathUtils.random(RANDOM_VOLUME_MIN, 1F),
            MathUtils.random(PITCH_MIN, PITCH_MAX),
            0F
        )
    }

    fun stopAll(assetsManager: GameAssetManager) {
        SoundDefinition.entries.forEach {
            assetsManager.getAllAssetsByDefinition(it).forEach { sound: Sound ->
                sound.stop()
            }
        }
    }

    fun stop(sound: Sound, id: Long) {
        sound.stop(id)
    }

    fun play(music: Music) {
        if (!GameDebugSettings.SFX) return

        music.play()
        music.volume = 0.05F
        music.isLooping = true
    }

    companion object {
        const val PITCH_MIN = 0.9F
        const val PITCH_MAX = 1.1F
        const val RANDOM_VOLUME_MIN = 0.6F
    }
}
