package com.gadarts.returnfire.ecs.systems.character.factories

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.graphics.g3d.Model
import com.badlogic.gdx.graphics.g3d.ModelInstance
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.utils.Disposable
import com.gadarts.returnfire.ecs.components.ComponentsMapper
import com.gadarts.returnfire.ecs.components.arm.ArmComponent
import com.gadarts.returnfire.ecs.components.character.CharacterColor
import com.gadarts.returnfire.ecs.components.model.GameModelInstance
import com.gadarts.returnfire.ecs.components.onboarding.BoardingAnimation
import com.gadarts.returnfire.ecs.systems.EntityBuilder
import com.gadarts.returnfire.ecs.systems.EntityBuilderImpl
import com.gadarts.returnfire.factories.GameModelInstanceFactory
import com.gadarts.returnfire.utils.GeneralUtils
import com.gadarts.shared.GameAssetManager
import com.gadarts.shared.assets.definitions.model.ModelDefinition
import com.gadarts.shared.assets.map.GameMapPlacedObject
import com.gadarts.shared.data.ImmutableGameModelInstanceInfo
import com.gadarts.shared.data.definitions.CharacterDefinition

abstract class CharacterFactory(
    protected val gameModelInstanceFactory: GameModelInstanceFactory,
    private val entityBuilder: EntityBuilder,
    private val assetsManager: GameAssetManager,
) : Disposable {

    abstract fun create(base: GameMapPlacedObject, color: CharacterColor): Entity

    protected fun addSpark(
        machineGunSparkModel: Model,
        relativePositionCalculator: ArmComponent.RelativePositionCalculator
    ): Entity {
        return entityBuilder.begin()
            .addModelInstanceComponent(
                GameModelInstance(
                    ModelInstance(machineGunSparkModel),
                    ImmutableGameModelInstanceInfo(ModelDefinition.MACHINE_GUN_SPARK),
                ),
                Vector3(),
                null,
                hidden = true
            )
            .addSparkComponent(relativePositionCalculator)
            .finishAndAddToEngine()
    }

    protected fun addCharacterBaseComponents(
        base: GameMapPlacedObject,
        characterDefinition: CharacterDefinition,
        primarySpark: Entity,
        secondarySpark: Entity?,
        primaryArmComponentCreator: () -> EntityBuilder,
        secondaryArmComponentCreator: (() -> EntityBuilder)?,
        boardingAnimation: BoardingAnimation?,
        color: CharacterColor
    ): GameModelInstance {
        val modelDefinition = characterDefinition.getModelDefinition()
        val gameModelInstance =
            gameModelInstanceFactory.createGameModelInstance(modelDefinition)
        entityBuilder.addModelInstanceComponent(
            gameModelInstance,
            auxVector3_1.set(
                base.column.toFloat() + 1F,
                -3.3F + assetsManager.getCachedBoundingBox(modelDefinition).height,
                base.row.toFloat() + 1F
            ),
            null,
        )
        entityBuilder.addDrowningEffectComponent()
        entityBuilder.addCharacterComponent(characterDefinition, color)
        entityBuilder.addBoardingCharacterComponent(
            color,
            boardingAnimation,
        )
        GeneralUtils.addColorComponent(
            entityBuilder,
            color
        )
        primaryArmComponentCreator()
        secondaryArmComponentCreator?.let { it() }
        ComponentsMapper.spark.get(primarySpark).parent = EntityBuilderImpl.entity!!
        secondarySpark?.let { ComponentsMapper.spark.get(secondarySpark).parent = EntityBuilderImpl.entity!! }
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
