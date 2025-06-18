package com.gadarts.shared.assets.map

import com.gadarts.shared.model.ElementType

data class GameMapPlacedObject(val definition: String, val type: ElementType, val row: Int, val column: Int)
