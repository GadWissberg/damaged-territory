package com.gadarts.returnfire.screens.hangar

import com.badlogic.gdx.graphics.g3d.ModelInstance
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.math.Vector3

class SelectableVehicle(
    val modelInstance: ModelInstance,
    val stage: ModelInstance,
    private val relativeHeight: Float,
    private val rotationAroundY: Float
) {
    val children = mutableListOf<SelectableVehicleChild>()

    fun addChild(child: SelectableVehicleChild) {
        children.add(child)
    }

    fun updateLocation() {
        translateToStage(modelInstance)
        children.forEach { translateToStage(it.modelInstance).translate(it.relativePosition) }
    }

    private fun translateToStage(model: ModelInstance): Matrix4 {
        return model.transform.setToTranslation(stage.transform.getTranslation(auxVector)).translate(
            0F,
            relativeHeight, 0F
        )
            .rotate(
                Vector3.Y, rotationAroundY
            )
    }

    companion object {
        private val auxVector = Vector3()
    }
}
