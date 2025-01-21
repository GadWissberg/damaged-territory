package com.gadarts.returnfire.components.onboarding

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.graphics.g3d.ModelInstance
import com.badlogic.gdx.graphics.g3d.attributes.BlendingAttribute
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.math.Vector3
import com.gadarts.returnfire.assets.definitions.SoundDefinition
import com.gadarts.returnfire.components.ComponentsMapper
import com.gadarts.returnfire.managers.GameAssetManager
import com.gadarts.returnfire.managers.SoundPlayer
import com.gadarts.returnfire.systems.EntityBuilder

class ApacheBoardingAnimation(private val entityBuilder: EntityBuilder) : BoardingAnimation {
    private var stage: Entity? = null
    private var done: Boolean = false
    private var firstUpdate: Boolean = true
    private var boardingSpeed: Float = 0.0f
    private var propellerRotationSpeed: Float = 0.0f

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
        val childModelInstanceComponent = ComponentsMapper.childModelInstance.get(character)
        val blending = childModelInstanceComponent.gameModelInstance.modelInstance.materials.get(0)
            .get(BlendingAttribute.Type) as BlendingAttribute
        handleFirstUpdateForLand(character, soundPlayer, assetsManager)
        updateRotation(childModelInstanceComponent.gameModelInstance.modelInstance, deltaTime * -1F * 0.5F)
        if (propellerRotationSpeed < ROTATION_THRESHOLD) {
            childModelInstanceComponent.visible = true
            blending.opacity += deltaTime
            blending.opacity = MathUtils.clamp(blending.opacity, 0F, 1F)
            updateDecalOpacity(character, deltaTime * -1F)
        }
        val transform = ComponentsMapper.modelInstance.get(character).gameModelInstance.modelInstance.transform
        val apachePosition = transform.getTranslation(auxVector1)
        if (apachePosition.y > GROUND_HEIGHT_POSITION) {
            takeStepForBoarding(deltaTime * -1F, transform)
        } else {
            transform.setTranslation(auxVector1.x, GROUND_HEIGHT_POSITION, auxVector1.z)
        }
        transform.getTranslation(apachePosition)
        if (stage != null) {
            val stagePosition =
                ComponentsMapper.modelInstance.get(stage).gameModelInstance.modelInstance.transform.getTranslation(
                    auxVector2
                )
            if (!apachePosition.epsilonEquals(stagePosition.x, apachePosition.y, stagePosition.z)) {
                auxVector3.set(apachePosition).lerp(stagePosition, 0.1F)
                transform.setTranslation(auxVector3.x, apachePosition.y, auxVector3.z)
            }
        }
        return propellerRotationSpeed == 0F
    }

    private fun handleFirstUpdateForLand(
        character: Entity,
        soundPlayer: SoundPlayer,
        assetsManager: GameAssetManager
    ) {
        if (firstUpdate) {
            val ambSoundComponent = ComponentsMapper.ambSound.get(character)
            soundPlayer.stop(ambSoundComponent.sound, ambSoundComponent.soundId)
            soundPlayer.play(
                assetsManager.getAssetByDefinition(SoundDefinition.PROPELLER_STOP),
                ComponentsMapper.modelInstance.get(character).gameModelInstance.modelInstance.transform.getTranslation(
                    auxVector1
                )
            )
            propellerRotationSpeed = MAX_ROTATION_SPEED
            firstUpdate = false
        }
    }

    private fun takeOff(
        deltaTime: Float,
        soundPlayer: SoundPlayer,
        assetsManager: GameAssetManager,
        character: Entity
    ): Boolean {
        val childModelInstance =
            ComponentsMapper.childModelInstance.get(character).gameModelInstance.modelInstance
        val blending = childModelInstance.materials.get(0).get(BlendingAttribute.Type) as BlendingAttribute
        if (firstUpdate) {
            init(null)
            ComponentsMapper.boarding.get(character).offBoardSoundId =
                soundPlayer.play(
                    assetsManager.getAssetByDefinition(SoundDefinition.PROPELLER_START),
                    ComponentsMapper.modelInstance.get(character).gameModelInstance.modelInstance.transform.getTranslation(
                        auxVector1
                    )
                )
            firstUpdate = false
        }
        updateRotation(childModelInstance, deltaTime)
        if (propellerRotationSpeed > ROTATION_THRESHOLD) {
            blending.opacity -= deltaTime
            updateDecalOpacity(character, deltaTime)
            val transform = ComponentsMapper.modelInstance.get(character).gameModelInstance.modelInstance.transform
            takeStepForBoarding(deltaTime, transform)
            val startHeight = ComponentsMapper.character.get(character).definition.getStartHeight()
            if (transform.getTranslation(auxVector1).y >= startHeight) {
                transform.setTranslation(auxVector1.x, startHeight, auxVector1.z)
                ComponentsMapper.childModelInstance.get(character).visible = false
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
        childModelInstance.transform.rotate(Vector3.Y, propellerRotationSpeed)
        propellerRotationSpeed += deltaTime * 4F
        propellerRotationSpeed = MathUtils.clamp(propellerRotationSpeed, 0F, MAX_ROTATION_SPEED)
    }

    override fun init(stage: Entity?) {
        firstUpdate = true
        boardingSpeed = 0F
        propellerRotationSpeed = 0F
        done = false
        this.stage = stage
    }


    companion object {
        private val auxVector1 = Vector3()
        private val auxVector2 = Vector3()
        private val auxVector3 = Vector3()
        private const val MAX_ROTATION_SPEED = 16F
        private const val ROTATION_THRESHOLD = 8F
        private const val GROUND_HEIGHT_POSITION = 0.2F
    }
}
