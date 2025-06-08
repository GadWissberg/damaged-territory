package com.gadarts.dte

import com.badlogic.gdx.graphics.PerspectiveCamera
import com.badlogic.gdx.graphics.g3d.Model
import com.badlogic.gdx.graphics.g3d.ModelInstance
import com.badlogic.gdx.utils.Disposable

class SharedData : Disposable {
    var selectedTile: TilesTypes? = null
    var selectedLayerIndex: Int = 1
    lateinit var camera: PerspectiveCamera
    val modelInstances = mutableListOf<ModelInstance>()
    val layers = mutableListOf<TileLayer>()
    lateinit var floorModel: Model
    override fun dispose() {
        floorModel.dispose()
    }
}
