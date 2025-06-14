package com.gadarts.dte.scene

import com.gadarts.dte.scene.handlers.render.EditorModelInstance
import com.gadarts.shared.model.definitions.AmbDefinition

data class PlacedObject(
    val row: Int,
    val column: Int,
    val definition: AmbDefinition,
    val modelInstance: EditorModelInstance
)
