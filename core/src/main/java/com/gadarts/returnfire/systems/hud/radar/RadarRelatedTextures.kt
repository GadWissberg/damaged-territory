package com.gadarts.returnfire.systems.hud.radar

import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.Disposable
import com.gadarts.returnfire.systems.hud.radar.RadarC.RADAR_SIZE
import com.gadarts.shared.GameAssetManager

class RadarRelatedTextures(assetsManager: GameAssetManager) : Disposable {
    val playerDot by lazy { assetsManager.getTexture("radar_character_brown") }
    val enemyDot by lazy { assetsManager.getTexture("radar_character_green") }
    val dashLine by lazy { TextureRegion(assetsManager.getTexture("radar_dash_line")) }
    val signatures by lazy {
        mapOf(
            0 to assetsManager.getTexture("radar_tile_water"),
            0b00000010 to assetsManager.getTexture("radar_tile_top"),
            0b01000000 to assetsManager.getTexture("radar_tile_bottom"),
            0b00001000 to assetsManager.getTexture("radar_tile_left"),
            0b00010000 to assetsManager.getTexture("radar_tile_right"),
            0b00010110 to assetsManager.getTexture("radar_tile_gulf_top_right"),
            0b00001011 to assetsManager.getTexture("radar_tile_gulf_top_left"),
            0b11010000 to assetsManager.getTexture("radar_tile_gulf_bottom_right"),
            0b01101000 to assetsManager.getTexture("radar_tile_gulf_bottom_left"),
            0b10000000 to assetsManager.getTexture("radar_tile_bottom_right"),
            0b00100000 to assetsManager.getTexture("radar_tile_bottom_left"),
            0b00000100 to assetsManager.getTexture("radar_tile_top_right"),
            0b00000001 to assetsManager.getTexture("radar_tile_top_left"),
            0b11111111 to assetsManager.getTexture("radar_tile_ground"),
        )
    }
    val scanLines = createScanlineTexture()
    val sweepTexture = createSweepTexture()

    private fun createSweepTexture(): Texture {
        val width = RADAR_SIZE.toInt()
        val height = 20
        val pixmap = Pixmap(width, height, Pixmap.Format.RGBA8888)

        for (y in 0 until height) {
            val alpha = y / height.toFloat()
            pixmap.setColor(0f, 1f, 0f, alpha * 0.3f)
            pixmap.drawLine(0, y, width, y)
        }

        val texture = Texture(pixmap)
        pixmap.dispose()
        return texture
    }

    private fun createScanlineTexture(): Texture {
        val width = RADAR_SIZE.toInt()
        val height = 8

        val pixmap = Pixmap(width, height, Pixmap.Format.RGBA8888)
        pixmap.setColor(0f, 0f, 0f, 0f)
        pixmap.fill()

        pixmap.setColor(0f, 0f, 0f, 0.3f)
        for (y in 0 until height step 4) {
            pixmap.drawLine(0, y, width, y)
        }

        val texture = Texture(pixmap)
        texture.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat)
        pixmap.dispose()

        return texture
    }

    override fun dispose() {
        scanLines.dispose()
    }

}