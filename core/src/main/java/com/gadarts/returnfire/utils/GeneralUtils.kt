package com.gadarts.returnfire.utils

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.PerspectiveCamera
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.math.collision.BoundingBox
import com.badlogic.gdx.utils.ScreenUtils
import com.gadarts.returnfire.components.ComponentsMapper

object GeneralUtils {

    val auxVector1 = Vector3()
    val auxVector2 = Vector3()

    fun createCamera(fov: Float): PerspectiveCamera {
        val perspectiveCamera = PerspectiveCamera(
            fov,
            Gdx.graphics.width.toFloat(),
            Gdx.graphics.height.toFloat()
        )
        perspectiveCamera.near = 0.1F
        perspectiveCamera.far = 300F
        return perspectiveCamera
    }

    fun clearScreen() {
        Gdx.gl.glViewport(
            0, 0,
            Gdx.graphics.backBufferWidth,
            Gdx.graphics.backBufferHeight
        )
        ScreenUtils.clear(Color.BLACK, true)
        Gdx.gl.glClearColor(0F, 0F, 0F, 1F)
        Gdx.gl.glClear(
            GL20.GL_COLOR_BUFFER_BIT
                or GL20.GL_DEPTH_BUFFER_BIT
                or if (Gdx.graphics.bufferFormat.coverageSampling) GL20.GL_COVERAGE_BUFFER_BIT_NV else 0
        )
    }

    fun getRandomPositionOnBoundingBox(bb: BoundingBox, bias: Float): Vector3 {
        val min = bb.min
        val max = bb.max
        val xRange = max.x - min.x
        val yRange = max.y - min.y
        val zRange = max.z - min.z
        val face = MathUtils.random(5)
        val position = auxVector1
        when (face) {
            0 -> position.set(
                min.x - bias,
                min.y + MathUtils.random() * yRange,
                min.z + MathUtils.random() * zRange
            )

            1 -> position.set(
                max.x + bias,
                min.y + MathUtils.random() * yRange,
                min.z + MathUtils.random() * zRange
            )

            2 -> position.set(
                min.x + MathUtils.random() * xRange,
                max.y + bias,
                min.z + MathUtils.random() * zRange
            )

            3 -> position.set(
                min.x + MathUtils.random() * xRange,
                min.y + MathUtils.random() * yRange,
                min.z + bias
            )

            4 -> position.set(
                min.x + MathUtils.random() * xRange,
                min.y + MathUtils.random() * yRange,
                max.z + bias
            )
        }

        return position
    }


    fun isBodyDisposed(entity: Entity): Boolean {
        return !ComponentsMapper.physics.has(entity) || ComponentsMapper.physics.get(entity).disposed
    }

}
