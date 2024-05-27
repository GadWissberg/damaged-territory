package com.gadarts.returnfire.model

import com.gadarts.returnfire.assets.ModelDefinition

enum class AmbModelDefinitions(
    private val modelDefinition: ModelDefinition,
    private val randomizeScale: Boolean = false,
    private val randomizeRotation: Boolean = false,
) : ElementsDefinitions {
    PALM_TREE(ModelDefinition.PALM_TREE, true, true),
    ROCK(ModelDefinition.ROCK, true, true),
    BUILDING(ModelDefinition.BUILDING),
    FENCE(ModelDefinition.FENCE),
    LIGHT_POLE(ModelDefinition.LIGHT_POLE),
    BARRIER(ModelDefinition.BARRIER),
    CABIN(ModelDefinition.CABIN),
    CAR(ModelDefinition.CAR),
    GUARD_HOUSE(ModelDefinition.GUARD_HOUSE),
    ANTENNA(ModelDefinition.ANTENNA),
    WATCH_TOWER(ModelDefinition.WATCH_TOWER),
    BUILDING_FLAG(ModelDefinition.BUILDING_FLAG);

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
