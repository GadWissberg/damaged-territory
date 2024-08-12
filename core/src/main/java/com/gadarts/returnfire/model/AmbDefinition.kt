package com.gadarts.returnfire.model

import com.gadarts.returnfire.assets.definitions.ModelDefinition

enum class AmbDefinition(
    private val modelDefinition: ModelDefinition,
    private val randomizeScale: Boolean = false,
    private val randomizeRotation: Boolean = false,
) : ElementsDefinitions {
    PALM_TREE(ModelDefinition.PALM_TREE, true, true),
    WATCH_TOWER(ModelDefinition.WATCH_TOWER),
    BUILDING_FLAG(ModelDefinition.BUILDING_FLAG),
    FLAG(ModelDefinition.FLAG),
    TURRET_CANNON(ModelDefinition.TURRET_BASE);

    override fun getModelDefinition(): ModelDefinition {
        return modelDefinition
    }

    override fun isRandomizeRotation(): Boolean {
        return randomizeRotation
    }

    override fun isRandomizeScale(): Boolean {
        return randomizeScale
    }
}
