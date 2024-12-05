package com.gadarts.returnfire.systems.player

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.graphics.g3d.Model
import com.badlogic.gdx.graphics.g3d.ModelInstance
import com.badlogic.gdx.math.Vector3
import com.gadarts.returnfire.assets.GameAssetManager
import com.gadarts.returnfire.assets.definitions.ModelDefinition
import com.gadarts.returnfire.components.ComponentsMapper
import com.gadarts.returnfire.components.arm.ArmComponent
import com.gadarts.returnfire.components.model.GameModelInstance
import com.gadarts.returnfire.components.onboarding.BoardingAnimation
import com.gadarts.returnfire.factories.GameModelInstanceFactory
import com.gadarts.returnfire.model.CharacterDefinition
import com.gadarts.returnfire.model.PlacedElement
import com.gadarts.returnfire.systems.EntityBuilder

abstract class CharacterFactory(
    private val assetsManager: GameAssetManager,
    protected val gameModelInstanceFactory: GameModelInstanceFactory,
    private val entityBuilder: EntityBuilder
) {

    abstract fun create(base: PlacedElement): Entity

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

    protected fun addPlayerBaseComponents(
        entityBuilder: EntityBuilder,
        base: PlacedElement,
        characterDefinition: CharacterDefinition,
        primarySpark: Entity,
        primaryArmComponentCreator: () -> EntityBuilder,
        boardingAnimation: BoardingAnimation?
    ): GameModelInstance {
        val gameModelInstance =
            gameModelInstanceFactory.createGameModelInstance(characterDefinition.getModelDefinition())
        entityBuilder.addModelInstanceComponent(
            gameModelInstance,
            auxVector3_1.set(base.col.toFloat() + 1F, -2.7F, base.row.toFloat() + 1F),
            null,
        )
        entityBuilder.addCharacterComponent(characterDefinition)
        entityBuilder.addOnboardingCharacterComponent(boardingAnimation)
        entityBuilder.addPlayerComponent()
        primaryArmComponentCreator()
        ComponentsMapper.spark.get(primarySpark).parent = EntityBuilder.entity!!
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
