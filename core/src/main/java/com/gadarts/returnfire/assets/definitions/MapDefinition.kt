package com.gadarts.returnfire.assets.definitions

import com.badlogic.gdx.assets.AssetLoaderParameters
import com.gadarts.returnfire.model.GameMap

enum class MapDefinition :
    AssetDefinition<GameMap> {

    MAP_0;


    private val paths = ArrayList<String>()

    init {
        initializePaths(MapDefinition.PATH_FORMAT)
    }

    override fun getPaths(): ArrayList<String> {
        return paths
    }

    override fun getParameters(): AssetLoaderParameters<GameMap>? {
        return null
    }

    override fun getClazz(): Class<GameMap> {
        return GameMap::class.java
    }

    override fun getDefinitionName(): String {
        return name
    }

    companion object {
        private const val PATH_FORMAT = "maps/%s.json"
    }

}
