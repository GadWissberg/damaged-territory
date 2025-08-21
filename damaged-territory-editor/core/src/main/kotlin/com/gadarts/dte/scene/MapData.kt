package com.gadarts.dte.scene

import com.gadarts.dte.TileLayer
import com.gadarts.dte.scene.handlers.render.EditorModelInstance

class MapData {
    val modelInstances = mutableListOf<EditorModelInstance>()
    val placedObjects = mutableListOf<PlacedObject>()
    val layers = mutableListOf<TileLayer>()

}
