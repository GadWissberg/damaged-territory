package com.gadarts.returnfire.utils

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.*
import com.badlogic.gdx.graphics.g3d.Material
import com.badlogic.gdx.graphics.g3d.attributes.BlendingAttribute
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.math.collision.BoundingBox
import com.badlogic.gdx.physics.bullet.dynamics.btDiscreteDynamicsWorld
import com.badlogic.gdx.physics.bullet.dynamics.btRigidBody
import com.badlogic.gdx.utils.ScreenUtils
import com.gadarts.returnfire.components.ComponentsMapper
import com.gadarts.returnfire.model.MapGraph
import kotlin.math.floor

object GeneralUtils {

    val auxVector1 = Vector3()
    val auxVector2 = Vector3()
    private val auxBoundingBox = BoundingBox()
    fun createFlatMesh(
        builder: ModelBuilder,
        meshName: String,
        size: Float,
        texture: Texture?,
        offset: Float = 0F
    ) {
        val material =
            if (texture != null) Material(TextureAttribute.createDiffuse(texture)) else Material(
                TextureAttribute(
                    TextureAttribute.Diffuse
                )
            )
        material.set(BlendingAttribute(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA))
        val mbp = builder.part(
            meshName,
            GL20.GL_TRIANGLES,
            (VertexAttributes.Usage.Position or VertexAttributes.Usage.Normal or VertexAttributes.Usage.TextureCoordinates).toLong(),
            material
        )
        mbp.setUVRange(0F, 0F, 1F, 1F)
        mbp.rect(
            offset + -size, 0F, offset + size,
            offset + size, 0F, offset + size,
            offset + size, 0F, offset + -size,
            offset + -size, 0F, offset + -size,
            0F, size, 0F,
        )
    }

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

    fun getTilesCoveredByBoundingBox(
        entity: Entity,
        mapGraph: MapGraph,
        resultFiller: (Int, Int, MapGraph) -> Unit,
    ) {
        val gameModelInstance = ComponentsMapper.modelInstance.get(entity).gameModelInstance
        val boundingBox = gameModelInstance.getBoundingBox(auxBoundingBox)
        val min = Vector3(boundingBox.min)
        val max = Vector3(boundingBox.max)
        val minX = floor(min.x).toInt()
        val minZ = floor(min.z).toInt()
        val maxX = floor(max.x).toInt()
        val maxZ = floor(max.z).toInt()
        for (x in minX..maxX) {
            for (z in minZ..maxZ) {
                val centerX = x + 0.5f
                val centerZ = z + 0.5f
                if (centerX in min.x..max.x && centerZ in min.z..max.z) {
                    resultFiller(x, z, mapGraph)
                }
            }
        }
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

    fun isEntityMarksNodeAsBlocked(entity: Entity): Boolean {
        return (ComponentsMapper.amb.has(entity) && ComponentsMapper.amb.get(entity).def.isMarksNodeAsBlocked())
            || (ComponentsMapper.character.has(entity) && ComponentsMapper.character.get(entity).definition.isMarksNodeAsBlocked())
    }

    fun getPositionOfModel(entity: Entity): Vector3 {
        return ComponentsMapper.modelInstance.get(entity).gameModelInstance.modelInstance.transform.getTranslation(
            auxVector1
        )
    }

    fun isBodyDisposed(entity: Entity): Boolean {
        return !ComponentsMapper.physics.has(entity) || ComponentsMapper.physics.get(entity).disposed
    }

    fun isBodyInWorld(btRigidBody: btRigidBody, collisionWorld: btDiscreteDynamicsWorld): Boolean {
        for (i in 0 until collisionWorld.numCollisionObjects) {
            if (collisionWorld.collisionObjectArray.atConst(i) == btRigidBody) {
                return true
            }
        }
        return false
    }

}
