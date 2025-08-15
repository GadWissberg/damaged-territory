package com.gadarts.dte.scene.handlers.render

import com.badlogic.gdx.graphics.g3d.ModelInstance

class EditorModelInstance(props: EditorModelInstanceProps) : ModelInstance(props.model) {
    val definition = props.definition
    var applyGray: Boolean = false
    override fun toString(): String {
        return (definition?.name ?: "ground") + " at " + transform.getTranslation(auxVector).toString() +
                " with gray: $applyGray"
    }

    companion object {
        private val auxVector = com.badlogic.gdx.math.Vector3()
    }
}
