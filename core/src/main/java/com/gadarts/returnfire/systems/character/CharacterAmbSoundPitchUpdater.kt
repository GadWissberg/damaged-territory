package com.gadarts.returnfire.systems.character

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.Family
import com.badlogic.ashley.core.PooledEngine
import com.badlogic.ashley.utils.ImmutableArray
import com.badlogic.gdx.math.MathUtils
import com.gadarts.returnfire.components.AmbSoundComponent
import com.gadarts.returnfire.components.ComponentsMapper

class CharacterAmbSoundPitchUpdater(engine: PooledEngine) {
    private val ambSoundEntities: ImmutableArray<Entity> by lazy {
        engine.getEntitiesFor(
            Family.all(AmbSoundComponent::class.java).get()
        )
    }

    fun update(deltaTime: Float) {
        for (entity in ambSoundEntities) {
            val ambSoundComponent = ComponentsMapper.ambSound.get(entity)
            val sound = ambSoundComponent.sound
            val pitchTarget = ambSoundComponent.pitchTarget
            val pitch = ambSoundComponent.pitch
            if (!MathUtils.isEqual(pitchTarget, pitch, 0.1F)) {
                val calculatePitchStep = calculateNewPitch(pitchTarget, pitch, deltaTime)
                ambSoundComponent.pitch = calculatePitchStep
                sound.setPitch(ambSoundComponent.soundId, calculatePitchStep)
            }
        }
    }

    private fun calculateNewPitch(pitchTarget: Float, pitch: Float, deltaTime: Float): Float {
        val stepSize = PITCH_STEP_SIZE * deltaTime * 60F * (if (pitch < pitchTarget) 1F else -1F)
        return pitch + stepSize
    }

    companion object {
        private const val PITCH_STEP_SIZE = 0.05F
    }
}
