package com.gadarts.returnfire.console

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle
import com.badlogic.gdx.scenes.scene2d.ui.Stack
import com.badlogic.gdx.utils.*
import com.gadarts.returnfire.assets.definitions.FontDefinition
import com.gadarts.returnfire.console.ConsoleConstants.TEXT_VIEW_NAME
import com.gadarts.returnfire.managers.GameAssetManager
import java.sql.Timestamp
import java.text.SimpleDateFormat
import java.util.*

class ConsoleOutputWindowHandler(
    private val console: Console,
    private val consoleTextures: ConsoleTextures,
    assetsManager: GameAssetManager
) : Disposable {
    private lateinit var scrollHandler: ConsoleScrollHandler
    val font: BitmapFont = assetsManager.getAssetByDefinition(FontDefinition.CONSOLA)
    val fontHeight: Float
    private lateinit var stage: Stage
    val stringBuilder: StringBuilder = StringBuilder()
    private val date = SimpleDateFormat("HH:mm:ss")
    private val timeStamp = Timestamp(TimeUtils.millis())
    val textStyle: LabelStyle = LabelStyle(font, Color.WHITE)
    private val arrow: Image by lazy { createArrow() }
    private lateinit var textWindowStack: Stack

    private fun createArrow(): Image {
        val arrow = Image(consoleTextures.arrowTexture)
        arrow.align = Align.bottomRight
        textWindowStack.add(arrow)
        arrow.setScaling(Scaling.none)
        arrow.setFillParent(false)
        arrow.isVisible = false
        return arrow
    }

    init {
        font.data.markupEnabled = true
        val layout = GlyphLayout()
        layout.setText(font, "test")
        fontHeight = layout.height
    }

    fun insertNewLog(text: String, logTime: Boolean, color: String?) {
        timeStamp.time = TimeUtils.millis()
        val colorText = if (Optional.ofNullable<String>(color).isPresent) color else OUTPUT_COLOR
        if (logTime) {
            appendTextWithTime(text, colorText)
        } else stringBuilder.append(colorText).append(text).append('\n')
        stringBuilder.append(OUTPUT_COLOR)
        (stage.root.findActor<Actor>(TEXT_VIEW_NAME) as Label).setText(stringBuilder)
        arrow.isVisible = false
    }

    private fun appendTextWithTime(text: String, colorText: String?) {
        stringBuilder.append("[").append(TIME_COLOR).append("]")
            .append(" [").append(date.format(timeStamp)).append("]: ")
            .append(colorText)
            .append(text).append('\n')
    }

    override fun dispose() {
        font.dispose()
    }

    fun init(scrollHandler: ConsoleScrollHandler, width: Float, height: Float, stage: Stage) {
        this.stage = stage
        this.scrollHandler = scrollHandler
        this.textWindowStack = Stack(scrollHandler.scrollPane)
        console.addUiWidget(textWindowStack).colspan(2).size(width, height).align(Align.bottomLeft).padRight(PADDING)
            .padLeft(PADDING).row()
    }

    companion object {
        private const val TIME_COLOR = "SKY"
        private const val OUTPUT_COLOR: String = "[LIGHT_GRAY]"
        private const val PADDING: Float = 10f
    }
}
