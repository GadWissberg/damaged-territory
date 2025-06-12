package com.gadarts.dte.scene

import com.badlogic.gdx.graphics.PerspectiveCamera
import com.badlogic.gdx.graphics.g3d.Model
import com.badlogic.gdx.utils.Disposable
import com.gadarts.dte.TileLayer
import com.gadarts.dte.scene.handlers.render.EditorModelInstance
import com.gadarts.shared.assets.definitions.external.TextureDefinition

class SharedData : Disposable {
    var selectedTile: TextureDefinition? = null
    var selectedLayerIndex: Int = 1
    lateinit var camera: PerspectiveCamera
    val modelInstances = mutableListOf<EditorModelInstance>()
    val layers = mutableListOf<TileLayer>()
    lateinit var floorModel: Model
    override fun dispose() {
        floorModel.dispose()
    }
}
