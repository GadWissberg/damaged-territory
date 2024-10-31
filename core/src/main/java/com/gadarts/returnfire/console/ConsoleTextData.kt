package com.gadarts.returnfire.console

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle
import com.badlogic.gdx.utils.Disposable
import com.badlogic.gdx.utils.StringBuilder
import com.badlogic.gdx.utils.TimeUtils
import com.gadarts.returnfire.assets.GameAssetManager
import com.gadarts.returnfire.assets.definitions.FontDefinition
import com.gadarts.returnfire.console.ConsoleConstants.TEXT_VIEW_NAME
import java.sql.Timestamp
import java.text.SimpleDateFormat
import java.util.*

class ConsoleTextData(assetsManager: GameAssetManager) : Disposable {
    val font: BitmapFont = assetsManager.getAssetByDefinition(FontDefinition.CONSOLA)
    val fontHeight: Float
    lateinit var stage: Stage
    val stringBuilder: StringBuilder = StringBuilder()
    private val date = SimpleDateFormat("HH:mm:ss")
    private val timeStamp = Timestamp(TimeUtils.millis())
    val textStyle: LabelStyle = LabelStyle(font, Color.WHITE)

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

    companion object {
        private const val TIME_COLOR = "SKY"
        private const val OUTPUT_COLOR: String = "[LIGHT_GRAY]"
    }
}
