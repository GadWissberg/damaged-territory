package com.gadarts.shared

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.PerspectiveCamera
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.VertexAttributes
import com.badlogic.gdx.graphics.g3d.Material
import com.badlogic.gdx.graphics.g3d.ModelInstance
import com.badlogic.gdx.graphics.g3d.attributes.BlendingAttribute
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.physics.bullet.collision.btBoxShape
import com.badlogic.gdx.physics.bullet.collision.btCollisionShape
import com.badlogic.gdx.physics.bullet.collision.btCompoundShape
import com.gadarts.shared.assets.utils.ModelCollisionShapeInfo

object SharedUtils {
    const val GAME_VIEW_FOV = 60F
    const val DROWNING_HEIGHT = -1F
    val auxVector = Vector3()
    const val INITIAL_INDEX_OF_TILES_MAPPING = 48
    val tilesChars = CharArray(80) { (it + INITIAL_INDEX_OF_TILES_MAPPING).toChar() }.joinToString("")

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

    fun calculateTileSignature(tileX: Int, tileZ: Int, bitMap: Array<Array<Int>>): Int {
        val mapWidth = bitMap[0].size
        val mapDepth = bitMap.size
        if (!(tileX in 0..<mapWidth && tileZ >= 0 && tileZ < mapDepth)) return 0

        val depth = mapDepth - 1
        var signature = if (bitMap[tileZ][tileX] == 1) 0b11111111 else 0
        val up = tileZ - 1
        val left = tileX - 1
        val right = tileX + 1
        val down = tileZ + 1
        if (tileX > 0 && tileZ > 0) {
            signature = signature or ((bitMap[up][left]) shl 7)
        }
        if (tileZ > 0) {
            signature = signature or ((bitMap[up][tileX]) shl 6)
        }
        if (tileX < mapWidth - 1 && tileZ > 0) {
            signature = signature or ((bitMap[up][right]) shl 5)
        }
        if (tileX > 0) {
            signature = signature or ((bitMap[tileZ][left]) shl 4)
        }
        if (tileX < mapWidth - 1) {
            signature = signature or ((bitMap[tileZ][right]) shl 3)
        }
        if (tileX > 0 && tileZ < depth - 1) {
            signature = signature or ((bitMap[down][left]) shl 2)
        }
        if (tileZ < depth - 1) {
            signature = signature or ((bitMap[down][tileX]) shl 1)
        }
        if (tileX < mapWidth - 1 && tileZ < depth - 1) {
            signature = signature or ((bitMap[down][right]) shl 0)
        }
        return signature
    }


    fun createFlatMesh(
        builder: ModelBuilder,
        meshName: String,
        size: Float,
        texture: Texture?,
        offset: Float = 0F,
        customMaterial: Material? = null
    ) {
        val material = customMaterial
            ?: if (texture != null) Material(TextureAttribute.createDiffuse(texture)) else Material(
                TextureAttribute(
                    TextureAttribute.Diffuse
                )
            )
        material.set(BlendingAttribute(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA, 1F))
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

    fun buildShapeFromModelCollisionShapeInfo(modelCollisionShapeInfo: ModelCollisionShapeInfo): btCollisionShape {
        val compoundShape = btCompoundShape()

        modelCollisionShapeInfo.collisionShapes.forEach { info ->
            val halfExtents = Vector3(info.dimensions).scl(0.5f)
            val boxShape = btBoxShape(halfExtents)
            val transform = Matrix4().setToTranslation(info.center)
            compoundShape.addChildShape(transform, boxShape)
        }

        return compoundShape
    }

    fun applyCustomTextureToModelInstance(
        assetsManager: GameAssetManager,
        modelInstance: ModelInstance,
        customTexture: String
    ) {
        val textureAttribute: TextureAttribute =
            modelInstance.materials.get(0).get(TextureAttribute.Diffuse) as TextureAttribute
        textureAttribute.textureDescription.texture = assetsManager.getTexture(customTexture)
    }

}
