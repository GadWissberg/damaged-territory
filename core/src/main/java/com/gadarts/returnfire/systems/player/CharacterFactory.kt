package com.gadarts.returnfire.systems.player

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.graphics.g3d.Model
import com.badlogic.gdx.graphics.g3d.ModelInstance
import com.badlogic.gdx.math.Vector3
import com.gadarts.returnfire.assets.GameAssetManager
import com.gadarts.returnfire.assets.definitions.ModelDefinition
import com.gadarts.returnfire.components.ArmComponent
import com.gadarts.returnfire.components.ComponentsMapper
import com.gadarts.returnfire.components.model.GameModelInstance
import com.gadarts.returnfire.model.CharacterDefinition
import com.gadarts.returnfire.model.PlacedElement
import com.gadarts.returnfire.screens.GamePlayScreen
import com.gadarts.returnfire.systems.EntityBuilder

abstract class CharacterFactory(private val assetsManager: GameAssetManager) {
    protected fun addSpark(
        machineGunSparkModel: Model,
        relativePositionCalculator: ArmComponent.RelativePositionCalculator
    ): Entity {
        return EntityBuilder.begin()
            .addModelInstanceComponent(
                GameModelInstance(ModelInstance(machineGunSparkModel), ModelDefinition.MACHINE_GUN_SPARK),
                Vector3(),
                null,
                hidden = true
            )
            .addSparkComponent(relativePositionCalculator)
            .finishAndAddToEngine()
    }

    private fun createPlayerModelInstance(): GameModelInstance {
        val modelDefinition = GamePlayScreen.SELECTED_VEHICLE.getModelDefinition()
        val model = assetsManager.getAssetByDefinition(modelDefinition)
        return GameModelInstance(
            ModelInstance(model),
            modelDefinition,
        )
    }

    protected fun addPlayerBaseComponents(
        entityBuilder: EntityBuilder,
        placedPlayer: PlacedElement,
        primarySpark: Entity,
        primaryArmComponentCreator: () -> EntityBuilder
    ) {
        val definition = placedPlayer.definition as CharacterDefinition
        entityBuilder.addModelInstanceComponent(
            createPlayerModelInstance(),
            auxVector3_1.set(placedPlayer.col.toFloat(), definition.getStartHeight(), placedPlayer.row.toFloat()),
            null,
        )
        entityBuilder.addCharacterComponent(GamePlayScreen.SELECTED_VEHICLE)
        entityBuilder.addPlayerComponent()
        primaryArmComponentCreator()
        ComponentsMapper.spark.get(primarySpark).parent = EntityBuilder.entity!!
    }

    protected fun createPrimarySpark(
        sparkModel: ModelDefinition,
        calculator: ArmComponent.RelativePositionCalculator
    ): Entity {
        return addSpark(assetsManager.getAssetByDefinition(sparkModel), calculator)
    }

    abstract fun create(placedPlayer: PlacedElement): Entity

    companion object {
        private val auxVector3_1 = Vector3()
    }
}
