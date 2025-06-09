package com.gadarts.dte.ui

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.gadarts.dte.TileLayer

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

    override fun draw(batch: Batch, parentAlpha: Float) {
        validate()

        val color = color
        val x = x
        val y = y
        val width = width
        val height = height

        batch.setColor(color.r, color.g, color.b, color.a * parentAlpha)

        val style = style
        val font = style.font
        val itemHeight = itemHeight
        val itemCount = items.size

        val yStart = y + height
        val selected = selectedIndex

        for (i in 0 until itemCount) {
            val itemY = yStart - itemHeight * (i + 1)

            if (i == selected) {
                style.selection?.draw(batch, x, itemY, width, itemHeight)
            }

            drawItem(batch, font, i, items[i], x, itemY + itemHeight * 0.75f, width)
        }
    }}
