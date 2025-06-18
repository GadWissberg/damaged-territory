package com.gadarts.shared.assets.map

data class GameMap(
    val layers: List<GameMapTileLayer>,
    val objects: List<GameMapPlacedObject>,
    val width: Int,
    val depth: Int
)
