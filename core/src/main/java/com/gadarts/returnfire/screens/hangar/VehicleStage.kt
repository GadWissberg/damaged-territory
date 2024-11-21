package com.gadarts.returnfire.screens.hangar

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.g3d.ModelInstance
import com.badlogic.gdx.math.Vector3

class VehicleStage(val modelInstance: ModelInstance) {
    private var deploy: Boolean = false

    fun updateLocation(delta: Float): Boolean {
        val position = modelInstance.transform.getTranslation(auxVector)
        if (deploy) {
            modelInstance.transform.trn(0f, 0F, -2f * delta)
            if (position.z < 0) {
                deploy = false
            }
        } else if (position.x == 0F) {
            modelInstance.transform.trn(0f, 2f * delta, 0F)
            val newPosition = modelInstance.transform.getTranslation(auxVector)
            if (newPosition.y >= 4F) {
                return true
            }
        } else if (position.x < 0) {
            modelInstance.transform.trn(2f * delta, 0f, 0f)
            val newPosition = modelInstance.transform.getTranslation(auxVector)
            if (newPosition.x > 0) {
                deploy()
            }
        } else if (position.x > 0) {
            modelInstance.transform.trn(-2f * delta, 0f, 0f)
            val newPosition = modelInstance.transform.getTranslation(auxVector)
            if (newPosition.x < 0) {
                deploy()
            }
        }
        return false
    }

    private fun deploy() {
        val newPosition = modelInstance.transform.getTranslation(auxVector)
        modelInstance.transform.setToTranslation(0F, newPosition.y, newPosition.z)
        deploy = true
    }

    companion object {
        private val auxVector = Vector3()
    }
}
