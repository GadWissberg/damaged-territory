package com.gadarts.shared.assets.map

import com.gadarts.shared.data.type.ElementType

data class GameMapPlacedObject(
    val definition: String,
    val type: ElementType,
    val row: Int,
    val column: Int,
    val rotation: Float?
)
