package com.gadarts.returnfire.systems.data

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.utils.Disposable
import com.gadarts.returnfire.managers.GameAssetManager
import com.gadarts.returnfire.model.GameMap

class GameSessionDataMap(assetsManager: GameAssetManager) : Disposable {
    val currentMap: GameMap =
        assetsManager.getAll(GameMap::class.java, com.badlogic.gdx.utils.Array())[0]
    lateinit var tilesEntities: Array<Array<Entity?>>
    override fun dispose() {

    }

}
