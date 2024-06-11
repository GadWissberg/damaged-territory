package com.gadarts.returnfire.assets.definitions

import com.badlogic.gdx.assets.AssetLoaderParameters
import com.badlogic.gdx.graphics.g3d.Model

enum class ModelDefinition(fileNames: Int = 1) : AssetDefinition<Model> {

    APACHE,
    BULLET,
    MISSILE,
    PALM_TREE(3),
    ROCK(3),
    BUILDING(3),
    FENCE,
    LIGHT_POLE,
    BARRIER,
    CABIN,
    CAR(3),
    GUARD_HOUSE,
    ANTENNA,
    WATCH_TOWER,
    BUILDING_FLAG,
    BUILDING_FLAG_DESTROYED,
    FLAG;

    private val paths = ArrayList<String>()

    init {
        initializePaths(ModelDefinition.PATH_FORMAT, fileNames)
    }

    override fun getPaths(): ArrayList<String> {
        return paths
    }

    override fun getParameters(): AssetLoaderParameters<Model>? {
        return null
    }

    override fun getClazz(): Class<Model> {
        return Model::class.java
    }

    override fun getDefinitionName(): String {
        return name
    }

    companion object {
        private const val PATH_FORMAT = "models/%s.g3dj"
    }
}
