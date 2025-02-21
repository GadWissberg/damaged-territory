package com.gadarts.returnfire.systems.data

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.g3d.decals.Decal
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector3
import com.gadarts.returnfire.managers.GameAssetManager

class BulletHolesHandler(assetsManager: GameAssetManager) {
    val holes: MutableList<Decal> = ArrayDeque()

    private val smallHolesTextureRegions by lazy {
        arrayOf(
            TextureRegion(assetsManager.getTexture("bullet_hole_small_0")),
            TextureRegion(assetsManager.getTexture("bullet_hole_small_1")),
            TextureRegion(assetsManager.getTexture("bullet_hole_small_2")),
        )
    }
    private val bigHolesTextureRegions by lazy {
        arrayOf(
            TextureRegion(assetsManager.getTexture("bullet_hole_big_0")),
            TextureRegion(assetsManager.getTexture("bullet_hole_big_1")),
            TextureRegion(assetsManager.getTexture("bullet_hole_big_2")),
        )
    }

    fun addSmall(position: Vector3) {
        addHole(position, smallHolesTextureRegions)
    }

    fun addBig(position: Vector3) {
        addHole(position, bigHolesTextureRegions)
    }

    private fun addHole(position: Vector3, textureRegions: Array<TextureRegion>) {
        val newDecal =
            Decal.newDecal(
                1F,
                1F,
                textureRegions[MathUtils.random(0, textureRegions.size - 1)],
                true
            )
        newDecal.setPosition(position.x, 0.01F, position.z)
        newDecal.rotateX(-90F)
        holes.add(newDecal)
        if (holes.size > 300) {
            holes.removeFirst()
        }
    }

}
