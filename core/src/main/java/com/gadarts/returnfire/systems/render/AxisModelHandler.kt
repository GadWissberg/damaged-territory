package com.gadarts.returnfire.systems.render

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.VertexAttributes
import com.badlogic.gdx.graphics.g3d.Material
import com.badlogic.gdx.graphics.g3d.Model
import com.badlogic.gdx.graphics.g3d.ModelBatch
import com.badlogic.gdx.graphics.g3d.ModelInstance
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.utils.Disposable

class AxisModelHandler : Disposable {
    init {
        createAxis()
    }

    companion object {
        internal val auxVector3a: Vector3 = Vector3()
        internal val auxVector3b: Vector3 = Vector3()
    }

    private lateinit var axisModelInstanceX: ModelInstance
    private lateinit var axisModelInstanceY: ModelInstance
    private lateinit var axisModelInstanceZ: ModelInstance
    private lateinit var axisModelX: Model
    private lateinit var axisModelY: Model
    private lateinit var axisModelZ: Model

    override fun dispose() {
        axisModelX.dispose()
        axisModelY.dispose()
        axisModelZ.dispose()
    }

    private fun transformAxisModel() {
        scaleAxis()
        axisModelInstanceX.transform.translate(0F, 0.1F, 0F)
        axisModelInstanceY.transform.translate(0F, 0.1F, 0F)
        axisModelInstanceZ.transform.translate(0F, 0.1F, 0F)
    }

    private fun scaleAxis() {
        axisModelInstanceX.transform.scale(2F, 2F, 2F)
        axisModelInstanceX.transform.translate(0F, 0.2F, 0F)
        axisModelInstanceY.transform.scale(2F, 2F, 2F)
        axisModelInstanceY.transform.translate(0F, 0.2F, 0F)
        axisModelInstanceZ.transform.scale(2F, 2F, 2F)
        axisModelInstanceZ.transform.translate(0F, 0.2F, 0F)
    }

    private fun createAxisModel(modelBuilder: ModelBuilder, dir: Vector3, color: Color): Model {
        return modelBuilder.createArrow(
            auxVector3a.setZero(),
            dir,
            Material(ColorAttribute.createDiffuse(color)),
            (VertexAttributes.Usage.Position or VertexAttributes.Usage.Normal).toLong()
        )
    }

    private fun createAxis() {
        val modelBuilder = ModelBuilder()
        axisModelX = createAxisModel(modelBuilder, auxVector3b.set(1F, 0F, 0F), Color.RED)
        axisModelInstanceX = ModelInstance(axisModelX)
        axisModelY = createAxisModel(modelBuilder, auxVector3b.set(0F, 1F, 0F), Color.GREEN)
        axisModelInstanceY = ModelInstance(axisModelY)
        axisModelZ = createAxisModel(modelBuilder, auxVector3b.set(0F, 0F, 1F), Color.BLUE)
        axisModelInstanceZ = ModelInstance(axisModelZ)
        transformAxisModel()
    }

    fun render(modelBatch: ModelBatch) {
        modelBatch.render(axisModelInstanceX)
        modelBatch.render(axisModelInstanceY)
        modelBatch.render(axisModelInstanceZ)
    }

}
