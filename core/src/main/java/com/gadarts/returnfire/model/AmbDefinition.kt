package com.gadarts.returnfire.model

import com.gadarts.returnfire.assets.definitions.ModelDefinition

enum class AmbDefinition(
    private val modelDefinition: ModelDefinition,
    private val randomizeScale: Boolean = false,
    private val randomizeRotation: Boolean = false,
) : ElementDefinition {
    PALM_TREE(ModelDefinition.PALM_TREE, true, true),
    WATCH_TOWER(ModelDefinition.WATCH_TOWER),
    BUILDING_FLAG(ModelDefinition.BUILDING_FLAG),
    FLAG(ModelDefinition.FLAG);

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
