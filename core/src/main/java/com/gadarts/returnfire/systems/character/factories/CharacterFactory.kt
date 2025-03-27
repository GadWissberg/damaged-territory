package com.gadarts.returnfire.systems.character.factories

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.graphics.g3d.Model
import com.badlogic.gdx.graphics.g3d.ModelInstance
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.utils.Disposable
import com.gadarts.returnfire.assets.definitions.ModelDefinition
import com.gadarts.returnfire.components.ComponentsMapper
import com.gadarts.returnfire.components.arm.ArmComponent
import com.gadarts.returnfire.components.character.CharacterColor
import com.gadarts.returnfire.components.model.GameModelInstance
import com.gadarts.returnfire.components.onboarding.BoardingAnimation
import com.gadarts.returnfire.factories.GameModelInstanceFactory
import com.gadarts.returnfire.managers.GameAssetManager
import com.gadarts.returnfire.model.PlacedElement
import com.gadarts.returnfire.model.definitions.CharacterDefinition
import com.gadarts.returnfire.systems.EntityBuilder
import com.gadarts.returnfire.systems.EntityBuilderImpl

abstract class CharacterFactory(
    protected val gameModelInstanceFactory: GameModelInstanceFactory,
    private val entityBuilder: EntityBuilder,
    private val assetsManager: GameAssetManager,
) : Disposable {

    abstract fun create(base: PlacedElement, color: CharacterColor): Entity

    protected fun addSpark(
        machineGunSparkModel: Model,
        relativePositionCalculator: ArmComponent.RelativePositionCalculator
    ): Entity {
        return entityBuilder.begin()
            .addModelInstanceComponent(
                GameModelInstance(ModelInstance(machineGunSparkModel), ModelDefinition.MACHINE_GUN_SPARK),
                Vector3(),
                null,
                hidden = true
            )
            .addSparkComponent(relativePositionCalculator)
            .finishAndAddToEngine()
    }

    protected fun addCharacterBaseComponents(
        base: PlacedElement,
        characterDefinition: CharacterDefinition,
        primarySpark: Entity,
        secondarySpark: Entity?,
        primaryArmComponentCreator: () -> EntityBuilder,
        secondaryArmComponentCreator: () -> EntityBuilder,
        boardingAnimation: BoardingAnimation?,
        color: CharacterColor
    ): GameModelInstance {
        val modelDefinition = characterDefinition.getModelDefinition()
        val gameModelInstance =
            gameModelInstanceFactory.createGameModelInstance(modelDefinition)
        entityBuilder.addModelInstanceComponent(
            gameModelInstance,
            auxVector3_1.set(
                base.col.toFloat() + 1F,
                -3.3F + assetsManager.getCachedBoundingBox(modelDefinition).height,
                base.row.toFloat() + 1F
            ),
            null,
        )
        entityBuilder.addCharacterComponent(characterDefinition, color)
        entityBuilder.addBoardingCharacterComponent(
            color,
            boardingAnimation,
        )
        primaryArmComponentCreator()
        secondaryArmComponentCreator()
        ComponentsMapper.spark.get(primarySpark).parent = EntityBuilderImpl.entity!!
        ComponentsMapper.spark.get(secondarySpark).parent = EntityBuilderImpl.entity!!
        return gameModelInstance
    }

    protected fun applyOpponentColor(
        character: Entity,
        color: CharacterColor,
        textureFileName: String
    ) {
        val textureAttribute =
            ComponentsMapper.modelInstance.get(character).gameModelInstance.modelInstance.materials.find {
                it.has(
                    TextureAttribute.Diffuse
                )
            }
                ?.get(TextureAttribute.Diffuse) as TextureAttribute
        textureAttribute.textureDescription.texture =
            assetsManager.getTexture("${textureFileName}_${color.name.lowercase()}")
    }

    companion object {
        private val auxVector3_1 = Vector3()
    }
}
