package com.gadarts.returnfire.screens.hangar

import com.badlogic.gdx.graphics.g3d.ModelInstance
import com.badlogic.gdx.math.Vector3
import com.gadarts.shared.data.definitions.CharacterDefinition

class VehicleStage(
    val modelInstance: ModelInstance,
    val characterDefinition: CharacterDefinition,
    private val initialPosition: Vector3
) {
    private var applyMovementForZ: Boolean = false

    //UGLY CODE!!!! Change to movement by a graph node
    fun updateLocation(delta: Float, deployingState: Int): Boolean {
        val position = modelInstance.transform.getTranslation(auxVector)
        val initialOnTheRight = initialPosition.x > 0
        if (applyMovementForZ) {
            takeStepForZ(delta, deployingState, position)
        } else if (position.x == 0F) {
            if (position.z <= 0F) {
                val reachedExit = takeStepForY(delta, deployingState)
                if (reachedExit) {
                    return true
                }
            } else if (deployingState < 0) {
                modelInstance.transform.trn(STAGE_STEP_SIZE * delta * (if (initialOnTheRight) 1 else -1), 0F, 0F)
            }
        } else if (position.x < 0) {
            modelInstance.transform.trn(STAGE_STEP_SIZE * delta * deployingState, 0f, 0f)
            val newPosition = modelInstance.transform.getTranslation(auxVector)
            if (deployingState > 0) {
                if (newPosition.x > 0) {
                    deploy()
                }
            } else {
                if (isReachedInitialPosition(initialOnTheRight, newPosition)) return true
            }
        } else if (position.x > 0) {
            modelInstance.transform.trn(-STAGE_STEP_SIZE * delta * deployingState, 0f, 0f)
            val newPosition = modelInstance.transform.getTranslation(auxVector)
            if (deployingState > 0) {
                if (newPosition.x < 0) {
                    deploy()
                }
            } else {
                if (isReachedInitialPosition(initialOnTheRight, newPosition)) return true
            }
        }
        return false
    }

    private fun isReachedInitialPosition(initialOnTheRight: Boolean, newPosition: Vector3): Boolean {
        if ((initialOnTheRight && newPosition.x >= initialPosition.x) || (!initialOnTheRight && newPosition.x <= initialPosition.x)) {
            modelInstance.transform.setTranslation(initialPosition)
            return true
        }
        return false
    }

    private fun takeStepForY(delta: Float, deployingState: Int): Boolean {
        modelInstance.transform.trn(0f, STAGE_STEP_SIZE * delta * deployingState, 0F)
        val newPosition = modelInstance.transform.getTranslation(auxVector)
        if (deployingState > 0) {
            if (newPosition.y >= 4F) {
                return true
            }
        }
        if (newPosition.y <= 0F) {
            modelInstance.transform.setTranslation(newPosition.x, 0F, newPosition.z)
            applyMovementForZ = true
        }
        return false
    }

    private fun takeStepForZ(delta: Float, deployingState: Int, position: Vector3) {
        modelInstance.transform.trn(0f, 0F, -STAGE_STEP_SIZE * delta * deployingState)
        if ((position.z <= 0 && deployingState > 0) || (position.z >= initialPosition.z && deployingState < 0)) {
            applyMovementForZ = false
        }
    }

    private fun deploy() {
        val newPosition = modelInstance.transform.getTranslation(auxVector)
        modelInstance.transform.setToTranslation(0F, newPosition.y, newPosition.z)
        applyMovementForZ = true
    }

    companion object {
        private val auxVector = Vector3()
        private const val STAGE_STEP_SIZE = 3F
    }
}
