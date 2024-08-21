package com.gadarts.returnfire.assets.definitions

import com.badlogic.gdx.assets.AssetLoaderParameters
import com.badlogic.gdx.graphics.g3d.Model

enum class ModelDefinition(fileNames: Int = 1) : AssetDefinition<Model> {

    APACHE,
    BULLET,
    MISSILE,
    PALM_TREE(3),
    WATCH_TOWER,
    BUILDING_FLAG,
    BUILDING_FLAG_DESTROYED,
    FLAG,
    TURRET_CANNON,
    TURRET_BASE,
    MACHINE_GUN_SPARK;

    private val pathFormat = "models/%s.g3dj"
    private val paths = ArrayList<String>()

    init {
        initializePaths(pathFormat, fileNames)
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

}
