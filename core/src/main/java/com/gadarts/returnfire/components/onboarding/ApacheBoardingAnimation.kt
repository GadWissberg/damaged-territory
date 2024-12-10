package com.gadarts.returnfire.components.onboarding

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.graphics.g3d.ModelInstance
import com.badlogic.gdx.graphics.g3d.attributes.BlendingAttribute
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.math.Vector3
import com.gadarts.returnfire.SoundPlayer
import com.gadarts.returnfire.assets.GameAssetManager
import com.gadarts.returnfire.assets.definitions.SoundDefinition
import com.gadarts.returnfire.components.ComponentsMapper
import com.gadarts.returnfire.systems.EntityBuilder

class ApacheBoardingAnimation(private val entityBuilder: EntityBuilder) : BoardingAnimation {
    private var done: Boolean = false
    private var firstUpdate: Boolean = true
    private var boardingSpeed: Float = 0.0f
    private var rotationSpeed: Float = 0.0f

    override fun update(
        deltaTime: Float,
        character: Entity,
        soundPlayer: SoundPlayer,
        assetsManager: GameAssetManager
    ): Boolean {
        val boardingComponent = ComponentsMapper.boarding.get(character)
        if (boardingComponent.isOffboarding()) {
            val done = takeOff(deltaTime, soundPlayer, assetsManager, character)
            if (done) {
                this.done = true
                return true
            }
        } else {
            val landed = land(deltaTime, soundPlayer, assetsManager, character)
            if (landed) {
                this.done = true
                return true
            }
        }
        return false
    }

    override fun isDone(): Boolean {
        return this.done
    }

    private fun land(
        deltaTime: Float,
        soundPlayer: SoundPlayer,
        assetsManager: GameAssetManager,
        character: Entity
    ): Boolean {
        val childModelInstanceComponent = ComponentsMapper.childModelInstanceComponent.get(character)
        val childModelInstance =
            childModelInstanceComponent.gameModelInstance.modelInstance
        val blending = childModelInstance.materials.get(0).get(BlendingAttribute.Type) as BlendingAttribute
        if (firstUpdate) {
            val ambSoundComponent = ComponentsMapper.ambSound.get(character)
            soundPlayer.stop(ambSoundComponent.sound, ambSoundComponent.soundId)
            soundPlayer.play(assetsManager.getAssetByDefinition(SoundDefinition.PROPELLER_STOP))
            rotationSpeed = MAX_ROTATION_SPEED
            firstUpdate = false
        }
        val negDeltaTime = deltaTime * -1F
        updateRotation(childModelInstance, negDeltaTime * 0.5F)
        if (rotationSpeed < ROTATION_THRESHOLD) {
            childModelInstanceComponent.visible = true
            blending.opacity += deltaTime
            blending.opacity = MathUtils.clamp(blending.opacity, 0F, 1F)
            updateDecalOpacity(character, negDeltaTime)
        }
        val transform = ComponentsMapper.modelInstance.get(character).gameModelInstance.modelInstance.transform
        if (transform.getTranslation(auxVector).y > GROUND_HEIGHT_POSITION) {
            takeStepForBoarding(negDeltaTime, transform)
        } else {
            transform.setTranslation(auxVector.x, GROUND_HEIGHT_POSITION, auxVector.z)
        }
        if (rotationSpeed == 0F) {
            return true
        }
        return false
    }

    private fun takeOff(
        deltaTime: Float,
        soundPlayer: SoundPlayer,
        assetsManager: GameAssetManager,
        character: Entity
    ): Boolean {
        val childModelInstance =
            ComponentsMapper.childModelInstanceComponent.get(character).gameModelInstance.modelInstance
        val blending = childModelInstance.materials.get(0).get(BlendingAttribute.Type) as BlendingAttribute
        if (firstUpdate) {
            reset()
            ComponentsMapper.boarding.get(character).offBoardSoundId =
                soundPlayer.play(assetsManager.getAssetByDefinition(SoundDefinition.PROPELLER_START))
            firstUpdate = false
        }
        updateRotation(childModelInstance, deltaTime)
        if (rotationSpeed > ROTATION_THRESHOLD) {
            blending.opacity -= deltaTime
            updateDecalOpacity(character, deltaTime)
            val transform = ComponentsMapper.modelInstance.get(character).gameModelInstance.modelInstance.transform
            takeStepForBoarding(deltaTime, transform)
            val startHeight = ComponentsMapper.character.get(character).definition.getStartHeight()
            if (transform.getTranslation(auxVector).y >= startHeight) {
                transform.setTranslation(auxVector.x, startHeight, auxVector.z)
                ComponentsMapper.childModelInstanceComponent.get(character).visible = false
                soundPlayer.stop(
                    assetsManager.getAssetByDefinition(SoundDefinition.PROPELLER_START),
                    ComponentsMapper.boarding.get(character).offBoardSoundId
                )
                entityBuilder.addAmbSoundComponentToEntity(
                    character,
                    assetsManager.getAssetByDefinition(SoundDefinition.PROPELLER)
                )
                return true
            }
        }
        return false
    }

    private fun takeStepForBoarding(deltaTime: Float, transform: Matrix4) {
        boardingSpeed += deltaTime * 0.0125F
        transform.translate(0F, boardingSpeed, 0F)
    }

    private fun updateDecalOpacity(character: Entity, deltaTime: Float) {
        val decal = ComponentsMapper.childDecal.get(character).decals[0].decal
        val color = decal.color
        val newAlpha = color.a + deltaTime
        decal.setColor(color.r, color.g, color.b, MathUtils.clamp(newAlpha, 0F, 1F))
    }

    private fun updateRotation(childModelInstance: ModelInstance, deltaTime: Float) {
        childModelInstance.transform.rotate(Vector3.Y, rotationSpeed)
        rotationSpeed += deltaTime * 4F
        rotationSpeed = MathUtils.clamp(rotationSpeed, 0F, MAX_ROTATION_SPEED)
    }

    override fun reset() {
        firstUpdate = true
        boardingSpeed = 0F
        rotationSpeed = 0F
        done = false
    }


    companion object {
        private val auxVector = Vector3()
        private const val MAX_ROTATION_SPEED = 16F
        private const val ROTATION_THRESHOLD = 8F
        private const val GROUND_HEIGHT_POSITION = 0.2F
    }
}
