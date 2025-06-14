package com.gadarts.dte

import com.gadarts.dte.scene.handlers.render.EditorModelInstance
import com.gadarts.shared.assets.definitions.external.TextureDefinition

data class PlacedTile(val modelInstance: EditorModelInstance, val definition: TextureDefinition)
