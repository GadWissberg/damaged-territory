package com.gadarts.returnfire.ecs.systems.data.map

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.utils.Disposable
import com.gadarts.returnfire.graph.MapGraph
import com.gadarts.shared.GameAssetManager
import com.gadarts.shared.assets.map.GameMap
import com.gadarts.shared.data.CharacterColor

class GameSessionDataMap(assetsManager: GameAssetManager) : Disposable {
    lateinit var groundBitMap: Array<Array<Int>>
    lateinit var mapGraph: MapGraph
    val externalSeaRegions = mutableListOf<LayerRegion>()
    val loadedMap: GameMap =
        assetsManager.getAll(GameMap::class.java, com.badlogic.gdx.utils.Array())[0]
    val tilesEntitiesByLayers by lazy {
        buildList {
            add(InGameTilesLayer(loadedMap.width, loadedMap.depth))
            addAll(
                loadedMap.layers.map { InGameTilesLayer(loadedMap.width, loadedMap.depth) }
            )
        }
    }

    lateinit
    var elevators: Map<CharacterColor, Entity>

    override fun dispose() {
        tilesEntitiesByLayers.forEach {
            it.layerRegions.forEach { row -> row.forEach { modelCache -> modelCache?.dispose() } }
        }
        externalSeaRegions.forEach { it.dispose() }
    }

}
