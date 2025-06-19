package com.gadarts.returnfire.utils

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.math.collision.BoundingBox
import com.gadarts.returnfire.components.ComponentsMapper
import com.gadarts.returnfire.model.MapGraph
import com.gadarts.shared.SharedUtils.tilesChars
import com.gadarts.shared.assets.definitions.external.ExternalDefinitions
import com.gadarts.shared.assets.definitions.external.TextureDefinition
import com.gadarts.shared.assets.map.GameMap
import com.gadarts.shared.assets.map.GameMapTileLayer
import com.gadarts.shared.assets.map.TilesMapping
import kotlin.math.floor

object MapUtils {
    private val auxBoundingBox = BoundingBox()
    private val auxVector1 = Vector3()
    private val auxVector2 = Vector3()

    fun determineTextureOfMapPosition(
        row: Int,
        col: Int,
        textureDefinitions: ExternalDefinitions<TextureDefinition>,
        gameMapTileLayer: GameMapTileLayer,
        gameMap: GameMap
    ): TextureDefinition {
        var indexOfFirst =
            tilesChars.indexOfFirst { c: Char -> gameMapTileLayer.tiles[gameMap.width * row + col] == c }
        if (indexOfFirst == -1) {
            indexOfFirst = 0
        }
        return textureDefinitions.definitions[TilesMapping.tiles[indexOfFirst]]!!
    }

    fun isEntityMarksNodeAsBlocked(entity: Entity): Boolean {
        return (ComponentsMapper.amb.has(entity) && ComponentsMapper.amb.get(entity).def.isMarksNodeAsBlocked())
                || (ComponentsMapper.character.has(entity) && ComponentsMapper.character.get(entity).definition.isMarksNodeAsBlocked())
    }

    fun getTilesCoveredByBoundingBox(
        entity: Entity,
        mapGraph: MapGraph,
        resultFiller: (Int, Int, MapGraph) -> Unit,
    ) {
        val gameModelInstance = ComponentsMapper.modelInstance.get(entity).gameModelInstance
        val boundingBox = gameModelInstance.getBoundingBox(auxBoundingBox)
        val min = auxVector1.set(boundingBox.min)
        val max = auxVector2.set(boundingBox.max)
        val minX = floor(min.x).toInt()
        val minZ = floor(min.z).toInt()
        val maxX = floor(max.x).toInt()
        val maxZ = floor(max.z).toInt()
        for (x in minX..maxX) {
            for (z in minZ..maxZ) {
                val centerX = x + 0.5f
                val centerZ = z + 0.5f
                if (centerX in min.x..max.x && centerZ in min.z..max.z) {
                    resultFiller(x, z, mapGraph)
                }
            }
        }
    }

}
