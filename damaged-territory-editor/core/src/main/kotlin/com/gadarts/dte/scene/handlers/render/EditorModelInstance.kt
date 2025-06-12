package com.gadarts.dte.scene.handlers.render

import com.badlogic.gdx.graphics.g3d.Model
import com.badlogic.gdx.graphics.g3d.ModelInstance

class EditorModelInstance(model: Model) : ModelInstance(model) {
    var applyGray: Boolean = false
}
