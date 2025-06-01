package com.gadarts.dte

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.VertexAttributes
import com.badlogic.gdx.graphics.g3d.Material
import com.badlogic.gdx.graphics.g3d.Model
import com.badlogic.gdx.graphics.g3d.ModelBatch
import com.badlogic.gdx.graphics.g3d.ModelInstance
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder
import com.badlogic.gdx.utils.Disposable

class AuxiliaryModels(private val mapSize: Int) : Disposable {
    private var gridModelInstance: ModelInstance
    private var axisModelInstance: ModelInstance
    private lateinit var gridModel: Model
    private val axisModel: Model

    init {
        val modelBuilder = ModelBuilder()
        axisModel = modelBuilder.createXYZCoordinates(
            1F, Material(ColorAttribute.createDiffuse(Color.RED)),
            ((VertexAttributes.Usage.Position or VertexAttributes.Usage.Normal).toLong())
        )
        axisModelInstance = ModelInstance(axisModel)
        gridModelInstance = addGrid(modelBuilder)
    }

    private fun addGrid(modelBuilder: ModelBuilder): ModelInstance {
        gridModel = modelBuilder.createLineGrid(
            mapSize,
            mapSize,
            1F,
            1F,
            Material(ColorAttribute.createDiffuse(Color.GRAY)),
            ((VertexAttributes.Usage.Position or VertexAttributes.Usage.Normal).toLong())
        )
        val gridModelInstance = ModelInstance(gridModel)
        gridModelInstance.transform.translate(
            mapSize / 2F,
            0.01F,
            mapSize / 2F
        )
        return gridModelInstance
    }


    fun render(batch: ModelBatch) {
        batch.render(axisModelInstance)
        batch.render(gridModelInstance)
    }

    override fun dispose() {
        GeneralUtils.disposeObject(this, AuxiliaryModels::class.java)

    }


}
