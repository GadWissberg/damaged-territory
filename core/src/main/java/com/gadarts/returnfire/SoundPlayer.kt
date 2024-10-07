package com.gadarts.returnfire

import com.badlogic.gdx.audio.Sound
import com.badlogic.gdx.math.MathUtils

class SoundPlayer {

    fun loopSound(sound: Sound): Long {
        if (!GameDebugSettings.SFX) return -1
        return sound.loop(0.27F, 1F, 0F)
    }

    fun play(sound: Sound): Long {
        if (!GameDebugSettings.SFX) return -1
        return sound.play(
            MathUtils.random(RANDOM_VOLUME_MIN, 1F),
            MathUtils.random(PITCH_MIN, PITCH_MAX),
            0F
        )
    }

    companion object {
        const val PITCH_MIN = 0.9F
        const val PITCH_MAX = 1.1F
        const val RANDOM_VOLUME_MIN = 0.6F
    }
}
