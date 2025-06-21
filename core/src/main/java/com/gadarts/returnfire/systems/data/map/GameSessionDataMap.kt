package com.gadarts.returnfire.systems.data.map

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.utils.Disposable
import com.gadarts.returnfire.components.character.CharacterColor
import com.gadarts.returnfire.model.MapGraph
import com.gadarts.shared.GameAssetManager
import com.gadarts.shared.assets.map.GameMap

class GameSessionDataMap(assetsManager: GameAssetManager) : Disposable {
    lateinit var groundBitMap: Array<Array<Int>>
    lateinit var mapGraph: MapGraph
    val loadedMap: GameMap =
        assetsManager.getAll(GameMap::class.java, com.badlogic.gdx.utils.Array())[0]
    val tilesEntitiesByLayers by lazy {
        buildList {
            add(TilesLayer(loadedMap.width, loadedMap.depth))
            addAll(
                loadedMap.layers.map { TilesLayer(loadedMap.width, loadedMap.depth) }
            )
        }
    }

    lateinit
    var stages: Map<CharacterColor, Entity>

    override fun dispose() {
        tilesEntitiesByLayers.forEach {
            it.modelCache.dispose()
        }
    }

}
