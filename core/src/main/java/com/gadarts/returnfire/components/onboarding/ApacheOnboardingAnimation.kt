package com.gadarts.returnfire.components.onboarding

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.graphics.g3d.attributes.BlendingAttribute
import com.badlogic.gdx.math.Vector3
import com.gadarts.returnfire.SoundPlayer
import com.gadarts.returnfire.assets.GameAssetManager
import com.gadarts.returnfire.assets.definitions.SoundDefinition
import com.gadarts.returnfire.components.ChildModelInstanceComponent
import com.gadarts.returnfire.components.ComponentsMapper
import com.gadarts.returnfire.systems.EntityBuilder
import kotlin.math.min

class ApacheOnboardingAnimation : OnboardingAnimation {
    private var firstUpdate: Boolean = true
    private var takeOffSpeed: Float = 0.0f
    private var rotationSpeed: Float = 0.0f

    override fun update(
        deltaTime: Float,
        character: Entity,
        soundPlayer: SoundPlayer,
        assetsManager: GameAssetManager
    ): Boolean {
        playPropellerStartSound(soundPlayer, assetsManager)
        val childModelInstance =
            ComponentsMapper.childModelInstanceComponent.get(character).gameModelInstance.modelInstance
        childModelInstance.transform.rotate(Vector3.Y, rotationSpeed)
        rotationSpeed += deltaTime * 4F
        if (rotationSpeed > 8F) {
            val blending = childModelInstance.materials.get(0).get(BlendingAttribute.Type) as BlendingAttribute
            blending.opacity -= deltaTime
            val decal = ComponentsMapper.childDecal.get(character).decals[0].decal
            val color = decal.color
            val newAlpha = color.a + deltaTime
            decal.setColor(color.r, color.g, color.b, min(newAlpha, 1F))
            val transform = ComponentsMapper.modelInstance.get(character).gameModelInstance.modelInstance.transform
            takeOffSpeed += deltaTime * 0.0125F
            transform.translate(
                0F,
                takeOffSpeed,
                0F
            )
            val startHeight = ComponentsMapper.character.get(character).definition.getStartHeight()
            if (transform.getTranslation(auxVector).y >= startHeight) {
                transform.setTranslation(auxVector.x, startHeight, auxVector.z)
                character.remove(ChildModelInstanceComponent::class.java)
                EntityBuilder.addAmbSoundComponent(
                    character,
                    assetsManager.getAssetByDefinition(SoundDefinition.PROPELLER)
                )
                return true
            }
        }
        return false
    }

    private fun playPropellerStartSound(
        soundPlayer: SoundPlayer,
        assetsManager: GameAssetManager
    ) {
        if (firstUpdate) {
            soundPlayer.play(assetsManager.getAssetByDefinition(SoundDefinition.PROPELLER_START))
            firstUpdate = false
        }
    }

    companion object {
        private val auxVector = Vector3()
    }
}
