package com.gadarts.returnfire.assets.definitions

import com.badlogic.gdx.assets.AssetLoaderParameters
import com.badlogic.gdx.graphics.g3d.Model
import com.badlogic.gdx.math.Vector3

enum class ModelDefinition(
    fileNames: Int = 1,
    val boundingBoxScale: Vector3 = Vector3(1F, 1F, 1F),
    val boundingBoxBias: Vector3 = Vector3.Zero
) :
    AssetDefinition<Model> {

    APACHE,
    BULLET,
    CANNON_BULLET,
    MISSILE,
    PALM_TREE(3),
    WATCH_TOWER,
    BUILDING_FLAG,
    BUILDING_FLAG_DESTROYED,
    FLAG,
    TURRET_CANNON(boundingBoxScale = Vector3(0.4F, 1F, 1F), boundingBoxBias = Vector3(0.2F, 0F, 0F)),
    TURRET_BASE,
    MACHINE_GUN_SPARK,
    CANNON_SPARK;

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
