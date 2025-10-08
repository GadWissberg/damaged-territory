package com.gadarts.returnfire.ecs.systems.data.map

import com.badlogic.ashley.core.Entity
import com.gadarts.returnfire.ecs.systems.data.map.LayerRegion.Companion.LAYER_REGION_SIZE
import kotlin.math.max

class InGameTilesLayer(width: Int, depth: Int) {
    val tilesEntities: Array<Array<Entity?>> = Array(depth) { Array(width) { null } }
    val layerRegions: Array<Array<LayerRegion?>> =
        Array(max(depth / LAYER_REGION_SIZE, 1)) { Array(max(width / LAYER_REGION_SIZE, 1)) { null } }

}
