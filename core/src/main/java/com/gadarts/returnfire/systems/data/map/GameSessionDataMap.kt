package com.gadarts.returnfire.systems.data.map

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.utils.Disposable
import com.gadarts.returnfire.components.character.CharacterColor
import com.gadarts.returnfire.model.MapGraph
import com.gadarts.shared.GameAssetManager
import com.gadarts.shared.assets.map.GameMap

class GameSessionDataMap(assetsManager: GameAssetManager) : Disposable {
    lateinit var bitMap: Array<Array<Int>>
    lateinit var mapGraph: MapGraph
    val currentMap: GameMap =
        assetsManager.getAll(GameMap::class.java, com.badlogic.gdx.utils.Array())[0]
    lateinit var tilesEntities: Array<Array<Entity?>>
    lateinit var stages: Map<CharacterColor, Entity>

    override fun dispose() {

    }

}
