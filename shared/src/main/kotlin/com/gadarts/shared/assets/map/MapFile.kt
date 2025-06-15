package com.gadarts.shared.assets.map

data class MapFile(val layers: List<MapFileLayer>, val objects: List<MapFileObject>, val width: Int, val depth: Int)
