package com.gadarts.shared

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.PerspectiveCamera
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.VertexAttributes
import com.badlogic.gdx.graphics.g3d.Material
import com.badlogic.gdx.graphics.g3d.attributes.BlendingAttribute
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder
import com.badlogic.gdx.math.Vector3

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


//    fun createFlatMesh(
//        builder: ModelBuilder,
//        meshName: String,
//        size: Float,
//        texture: Texture?,
//        offset: Float = 0F,
//        customMaterial: Material? = null,
//        gridDivisions: Int = 8,
//        noiseAmplitude: Float = 0.1F
//    ) {
//        val material = customMaterial
//            ?: if (texture != null) Material(TextureAttribute.createDiffuse(texture)) else Material(
//                TextureAttribute(TextureAttribute.Diffuse)
//            )
//
//        material.set(BlendingAttribute(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA, 1F))
//
//        val step = (2f * size) / gridDivisions
//        val halfSize = size
//
//        fun isEdge(x: Int, z: Int): Boolean = x == 0 || z == 0 || x == gridDivisions || z == gridDivisions
//
//        val heights = Array(gridDivisions + 1) { FloatArray(gridDivisions + 1) }
//
//        for (x in 0..gridDivisions) {
//            for (z in 0..gridDivisions) {
//                heights[x][z] = if (isEdge(x, z)) 0f else (if (MathUtils.randomBoolean()) MathUtils.random(
//                    -noiseAmplitude,
//                    noiseAmplitude
//                ) else 0F)
//            }
//        }
//
//        val positions = Array(gridDivisions + 1) { x ->
//            Array(gridDivisions + 1) { z ->
//                Vector3(offset - halfSize + x * step, heights[x][z], offset - halfSize + z * step)
//            }
//        }
//
//        val normals = Array(gridDivisions + 1) { Array(gridDivisions + 1) { Vector3.Zero.cpy() } }
//
//        // Compute normals per vertex
//        for (x in 1 until gridDivisions) {
//            for (z in 1 until gridDivisions) {
//                val normal = Vector3()
//                val p = positions[x][z]
//
//                val neighbors = listOf(
//                    positions[x - 1][z],
//                    positions[x + 1][z],
//                    positions[x][z - 1],
//                    positions[x][z + 1]
//                )
//
//                neighbors.forEachIndexed { i, neighbor ->
//                    val next = neighbors[(i + 1) % neighbors.size]
//                    val edge1 = Vector3(neighbor).sub(p)
//                    val edge2 = Vector3(next).sub(p)
//                    normal.add(edge1.crs(edge2).nor())
//                }
//
//                normals[x][z] = normal.nor()
//            }
//        }
//
//        // Edge normals (set to Vector3.Y)
//        for (x in 0..gridDivisions) {
//            normals[x][0].set(Vector3.Y)
//            normals[x][gridDivisions].set(Vector3.Y)
//        }
//        for (z in 0..gridDivisions) {
//            normals[0][z].set(Vector3.Y)
//            normals[gridDivisions][z].set(Vector3.Y)
//        }
//
//        val meshBuilder = MeshBuilder()
//        meshBuilder.begin(
//            VertexAttributes(
//                VertexAttribute.Position(),
//                VertexAttribute.Normal(),
//                VertexAttribute.TexCoords(0)
//            ), GL20.GL_TRIANGLES
//        )
//
//        val v00 = VertexInfo()
//        val v10 = VertexInfo()
//        val v11 = VertexInfo()
//        val v01 = VertexInfo()
//
//        for (x in 0 until gridDivisions) {
//            for (z in 0 until gridDivisions) {
//                val u0 = x.toFloat() / gridDivisions
//                val v0 = z.toFloat() / gridDivisions
//                val u1 = (x + 1).toFloat() / gridDivisions
//                val v1 = (z + 1).toFloat() / gridDivisions
//
//                v00.set(positions[x][z], normals[x][z], null, null).setUV(u0, v0)
//                v10.set(positions[x + 1][z], normals[x + 1][z], null, null).setUV(u1, v0)
//                v11.set(positions[x + 1][z + 1], normals[x + 1][z + 1], null, null).setUV(u1, v1)
//                v01.set(positions[x][z + 1], normals[x][z + 1], null, null).setUV(u0, v1)
//
//                meshBuilder.rect(v01, v11, v10, v00)
//            }
//        }
//
//        val mesh = meshBuilder.end()
//        builder.part(meshName, mesh, GL20.GL_TRIANGLES, material)
//    }
}
