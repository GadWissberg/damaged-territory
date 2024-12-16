package com.gadarts.returnfire.systems.player

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.graphics.g3d.Model
import com.badlogic.gdx.graphics.g3d.ModelInstance
import com.badlogic.gdx.math.Vector3
import com.gadarts.returnfire.assets.definitions.ModelDefinition
import com.gadarts.returnfire.components.ComponentsMapper
import com.gadarts.returnfire.components.arm.ArmComponent
import com.gadarts.returnfire.components.character.CharacterColor
import com.gadarts.returnfire.components.model.GameModelInstance
import com.gadarts.returnfire.components.onboarding.BoardingAnimation
import com.gadarts.returnfire.factories.GameModelInstanceFactory
import com.gadarts.returnfire.managers.GameAssetManager
import com.gadarts.returnfire.model.CharacterDefinition
import com.gadarts.returnfire.model.PlacedElement
import com.gadarts.returnfire.systems.EntityBuilder
import com.gadarts.returnfire.systems.EntityBuilderImpl

abstract class CharacterFactory(
    private val assetsManager: GameAssetManager,
    protected val gameModelInstanceFactory: GameModelInstanceFactory,
    private val entityBuilder: EntityBuilder,
) {

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
        primaryArmComponentCreator: () -> EntityBuilder,
        boardingAnimation: BoardingAnimation?,
        color: CharacterColor
    ): GameModelInstance {
        val gameModelInstance =
            gameModelInstanceFactory.createGameModelInstance(characterDefinition.getModelDefinition())
        entityBuilder.addModelInstanceComponent(
            gameModelInstance,
            auxVector3_1.set(base.col.toFloat() + 1F, -2.7F, base.row.toFloat() + 1F),
            null,
        )
        entityBuilder.addCharacterComponent(characterDefinition, color)
        entityBuilder.addBoardingCharacterComponent(
            color,
            boardingAnimation,
        )
        primaryArmComponentCreator()
        ComponentsMapper.spark.get(primarySpark).parent = EntityBuilderImpl.entity!!
        return gameModelInstance
    }

    protected fun createPrimarySpark(
        sparkModel: ModelDefinition,
        calculator: ArmComponent.RelativePositionCalculator
    ): Entity {
        return addSpark(assetsManager.getAssetByDefinition(sparkModel), calculator)
    }

    companion object {
        private val auxVector3_1 = Vector3()
    }
}
