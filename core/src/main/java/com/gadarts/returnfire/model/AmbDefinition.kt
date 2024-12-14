package com.gadarts.returnfire.model

import com.badlogic.gdx.physics.bullet.collision.btCollisionObject.CollisionFlags
import com.gadarts.returnfire.assets.definitions.ModelDefinition

enum class AmbDefinition(
    private val modelDefinition: ModelDefinition,
    private val randomizeScale: Boolean = false,
    private val randomizeRotation: Boolean = false,
    val collisionFlags: Int = CollisionFlags.CF_STATIC_OBJECT,
    val placeInMiddleOfCell: Boolean = true,
) : ElementDefinition {
    PALM_TREE(ModelDefinition.PALM_TREE, true, true),
    WATCH_TOWER(ModelDefinition.WATCH_TOWER),
    BUILDING_FLAG(ModelDefinition.BUILDING_FLAG),
    FLAG(ModelDefinition.FLAG),
    BASE_BROWN(
        modelDefinition = ModelDefinition.PIT,
        collisionFlags = -1,
        placeInMiddleOfCell = false
    ),
    BASE_GREEN(
        modelDefinition = ModelDefinition.PIT,
        collisionFlags = -1,
        placeInMiddleOfCell = false
    );


    override fun getModelDefinition(): ModelDefinition {
        return modelDefinition
    }

    override fun getName(): String {
        return name
    }

    override fun getType(): ElementType {
        return ElementType.AMB
    }

    override fun isRandomizeRotation(): Boolean {
        return randomizeRotation
    }

    override fun isRandomizeScale(): Boolean {
        return randomizeScale
    }
}
