package com.gadarts.returnfire.ecs.systems.render

import com.badlogic.gdx.graphics.g3d.ModelBatch
import com.badlogic.gdx.graphics.g3d.decals.DecalBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.utils.Disposable

data class RenderSystemBatches(
    val decalBatch: DecalBatch,
    val modelBatch: ModelBatch,
    val shadowBatch: ModelBatch,
    val shapeRenderer: ShapeRenderer
) :
    Disposable {
    override fun dispose() {
        decalBatch.dispose()
        modelBatch.dispose()
        shadowBatch.dispose()
    }

}
