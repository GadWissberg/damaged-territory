package com.gadarts.returnfire.model

import com.gadarts.returnfire.model.definitions.ElementDefinition

class PlacedElement(
    val definition: ElementDefinition,
    val row: Int,
    val col: Int,
    val direction: Int
)
