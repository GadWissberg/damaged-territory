package com.gadarts.returnfire.managers

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.audio.Music
import com.badlogic.gdx.audio.Sound
import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector3
import com.gadarts.returnfire.GameDebugSettings
import com.gadarts.returnfire.ecs.components.ComponentsMapper
import com.gadarts.shared.GameAssetManager
import com.gadarts.shared.assets.definitions.SoundDefinition

class SoundManager(private val assetsManager: GameAssetManager, private val runsOnMobile: Boolean) {

    private var camera: Camera? = null

    fun sessionInitialize(camera: Camera) {
        this.camera = camera
    }

    fun loopSound(sound: Sound): Long {
        if (!GameDebugSettings.SFX) return -1
        return sound.loop(1F, 1F, 0F)
    }

    fun play(sound: SoundDefinition): Long {
        return play(assetsManager.getAssetByDefinition(sound), null, false)
    }

    fun play(sound: Sound, sourcePosition: Vector3? = null, randomPitch: Boolean = true): Long {
        if (!GameDebugSettings.SFX) return -1

        val affectedByPosition =
            if (sourcePosition != null) calculateVolumeBasedOnPosition(sourcePosition) else 1F
        val volume = MathUtils.random(RANDOM_VOLUME_MIN, 1F) * affectedByPosition
        return sound.play(
            volume,
            if (randomPitch) MathUtils.random(PITCH_MIN, PITCH_MAX) else 1F,
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
        if (!GameDebugSettings.SFX || GameDebugSettings.DISABLE_MUSIC) return

        music.play()
        music.volume = if (runsOnMobile) 0.05F else 0.2F
        music.isLooping = true
    }

    fun calculateVolumeBasedOnPosition(position: Vector3): Float {
        if (camera != null) {
            val distance = camera!!.position.dst2(position)
            return if (distance > 100) (1F / (1 + 0.007F * distance)) else 1F
        }
        return 0F
    }

    fun playWithSource(sound: SoundDefinition, source: Entity): Long {
        return play(
            assetsManager.getAssetByDefinition(sound),
            ComponentsMapper.modelInstance.get(source).gameModelInstance.modelInstance.transform.getTranslation(
                auxVector
            )
        )
    }

    companion object {
        const val PITCH_MIN = 0.9F
        const val PITCH_MAX = 1.1F
        const val RANDOM_VOLUME_MIN = 0.6F
        private val auxVector = Vector3()
    }
}
