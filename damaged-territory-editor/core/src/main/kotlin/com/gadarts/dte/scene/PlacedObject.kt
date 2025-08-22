package com.gadarts.dte.scene

import com.gadarts.dte.scene.handlers.render.EditorModelInstance
import com.gadarts.shared.data.definitions.ElementDefinition

data class PlacedObject(
    val row: Int,
    val column: Int,
    val definition: ElementDefinition,
    val modelInstance: EditorModelInstance
)
