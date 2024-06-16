package com.gadarts.returnfire.systems.render

import com.badlogic.gdx.graphics.g3d.ModelBatch
import com.badlogic.gdx.graphics.g3d.decals.DecalBatch
import com.badlogic.gdx.utils.Disposable

class RenderSystemBatches(val decalBatch: DecalBatch, val modelBatch: ModelBatch, val shadowBatch: ModelBatch) :
    Disposable {
    override fun dispose() {
        decalBatch.dispose()
        modelBatch.dispose()
        shadowBatch.dispose()
    }

}
