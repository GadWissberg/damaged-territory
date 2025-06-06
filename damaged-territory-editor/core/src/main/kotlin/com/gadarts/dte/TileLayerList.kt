package com.gadarts.dte

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener

class TileLayerList(skin: Skin) : com.badlogic.gdx.scenes.scene2d.ui.List<TileLayer>(skin) {
    init {
        addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                if (selected != null && selected.disabled) {
                    selectedIndex = 1
                }
            }
        })
    }

    override fun drawItem(
        batch: Batch,
        font: BitmapFont,
        index: Int,
        item: TileLayer,
        x: Float,
        y: Float,
        width: Float
    ): GlyphLayout {
        font.color = if (item.disabled) Color.GRAY else Color.WHITE
        val layout = GlyphLayout(font, item.name)
        font.draw(batch, layout, x, y)
        return layout
    }

    override fun getItemAt(y: Float): TileLayer? {
        val item = super.getItemAt(y)
        return if (item != null && item.disabled) null else item
    }
}