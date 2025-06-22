package com.gadarts.returnfire.systems.data.map

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.graphics.g3d.ModelCache

class InGameTilesLayer(width: Int, depth: Int) {
    val tilesEntities: Array<Array<Entity?>> = Array(depth) { Array(width) { null } }
    val modelCache = ModelCache()
}
