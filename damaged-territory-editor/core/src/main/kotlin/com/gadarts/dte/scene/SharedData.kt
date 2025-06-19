package com.gadarts.dte.scene

import com.badlogic.gdx.graphics.PerspectiveCamera
import com.badlogic.gdx.graphics.g3d.Model
import com.badlogic.gdx.utils.Disposable
import com.gadarts.dte.TileLayer
import com.gadarts.dte.scene.handlers.render.EditorModelInstance
import com.gadarts.dte.ui.Modes
import com.gadarts.shared.assets.definitions.external.TextureDefinition
import com.gadarts.shared.model.definitions.ElementDefinition

class SharedData : Disposable {
    var selectedObject: ElementDefinition? = null
    var selectedMode: Modes = Modes.TILES
    var selectedTile: TextureDefinition? = null
    var selectedLayerIndex: Int = 1
    lateinit var camera: PerspectiveCamera
    val modelInstances = mutableListOf<EditorModelInstance>()
    val layers = mutableListOf<TileLayer>()
    val placedObjects = mutableListOf<PlacedObject>()
    lateinit var floorModel: Model
    override fun dispose() {
        floorModel.dispose()
    }
}
