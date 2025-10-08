package com.gadarts.returnfire.ecs.systems.data.map

import com.badlogic.gdx.graphics.g3d.ModelCache
import com.badlogic.gdx.math.Vector3

class LayerRegion(private val x: Float, private val z: Float, private val width: Int, private val depth: Int) :
    ModelCache() {
    val boundingBox = com.badlogic.gdx.math.collision.BoundingBox()

    override fun end() {
        super.end()
        boundingBox.set(
            Vector3(x, 0f, z),
            Vector3(x + width.toFloat(), 32F, z + depth.toFloat())
        )
    }

    companion object {
        const val LAYER_REGION_SIZE = 8
    }

}
