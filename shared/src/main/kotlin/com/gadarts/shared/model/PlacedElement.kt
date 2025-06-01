package com.gadarts.shared.model

import com.gadarts.shared.model.definitions.ElementDefinition

class PlacedElement(
    val definition: ElementDefinition,
    val row: Int,
    val col: Int,
    val direction: Int
)
