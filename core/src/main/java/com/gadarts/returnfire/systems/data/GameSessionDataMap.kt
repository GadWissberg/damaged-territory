package com.gadarts.returnfire.systems.data

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.utils.Disposable
import com.gadarts.returnfire.components.character.CharacterColor
import com.gadarts.returnfire.managers.GameAssetManager
import com.gadarts.returnfire.model.GameMap
import com.gadarts.returnfire.model.GraphMap

class GameSessionDataMap(assetsManager: GameAssetManager) : Disposable {
    lateinit var graphMap: GraphMap
    val currentMap: GameMap =
        assetsManager.getAll(GameMap::class.java, com.badlogic.gdx.utils.Array())[0]
    lateinit var tilesEntities: Array<Array<Entity?>>
    lateinit var stages: Map<CharacterColor, Entity>

    override fun dispose() {

    }

    companion object {
        const val DROWNING_HEIGHT = -0.5F
    }
}
