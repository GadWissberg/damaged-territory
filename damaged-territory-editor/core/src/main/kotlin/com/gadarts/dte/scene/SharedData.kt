package com.gadarts.dte.scene

import com.badlogic.gdx.graphics.PerspectiveCamera
import com.badlogic.gdx.graphics.g3d.Model
import com.badlogic.gdx.utils.Disposable

class SharedData : Disposable {
    val selectionData = SelectionData()
    val mapData = MapData()
    lateinit var camera: PerspectiveCamera
    lateinit var floorModel: Model

    override fun dispose() {
        floorModel.dispose()
    }
}
