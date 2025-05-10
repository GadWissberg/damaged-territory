package com.gadarts.returnfire.console

import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane
import com.badlogic.gdx.utils.Align
import com.gadarts.returnfire.console.ConsoleConstants.TEXT_VIEW_NAME

class ConsoleScrollHandler(
    private val consoleOutputWindowHandler: ConsoleOutputWindowHandler,
    private val consoleTextures: ConsoleTextures
) {
    private var scrollToEnd: Boolean = false
    lateinit var scrollPane: ScrollPane
    fun scroll(units: Float) {
        scrollPane.scrollY += units
        scrollToEnd = false
    }

    private fun createScrollPane(stage: Stage): ScrollPane {
        val textStyle: LabelStyle = consoleOutputWindowHandler.textStyle
        textStyle.background = consoleTextures.textBackgroundTextureRegionDrawable
        val textView = Label(consoleOutputWindowHandler.stringBuilder, textStyle)
        textView.setAlignment(Align.bottomLeft)
        textView.name = TEXT_VIEW_NAME
        textView.wrap = true
        val scrollPane = ScrollPane(textView)
        scrollPane.touchable = Touchable.disabled
        stage.addActor(scrollPane)
        return scrollPane
    }

    fun scrollToEnd() {
        scrollToEnd = true
    }

    fun init(stage: Stage) {
        scrollPane = createScrollPane(stage)
    }
}
