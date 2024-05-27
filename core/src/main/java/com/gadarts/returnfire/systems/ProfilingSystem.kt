package com.gadarts.returnfire.systems

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.profiling.GLProfiler
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.gadarts.returnfire.GameDebugSettings
import com.gadarts.returnfire.Services
import com.gadarts.returnfire.systems.events.SystemEvents

@Suppress("GDXKotlinProfilingCode")
class ProfilingSystem : GameEntitySystem() {

    private val stringBuilder: StringBuilder = StringBuilder()
    private lateinit var glProfiler: GLProfiler
    private lateinit var label: Label

    override val subscribedEvents: Map<SystemEvents, HandlerOnEvent> = emptyMap()
    override fun resume(delta: Long) {

    }

    override fun dispose() {
        gameSessionData.stage.dispose()
    }

    override fun initialize(gameSessionData: GameSessionData, services: Services) {
        super.initialize(gameSessionData, services)
        glProfiler = GLProfiler(Gdx.graphics)
        setGlProfiler()
        addLabel()
    }

    private fun addLabel() {
        val font = BitmapFont()
        font.data.setScale(2f)
        val style = Label.LabelStyle(font, Color.WHITE)
        label = Label(stringBuilder, style)
        label.setPosition(0f, (Gdx.graphics.height - 175).toFloat())
        gameSessionData.stage.addActor(label)
        label.zIndex = 0
    }

    private fun setGlProfiler() {
        if (GameDebugSettings.SHOW_GL_PROFILING) {
            glProfiler.enable()
        }
    }

    override fun update(delta: Float) {
        if (glProfiler.isEnabled) {
            stringBuilder.setLength(0)
            displayLine(LABEL_FPS, Gdx.graphics.framesPerSecond)
            displayGlProfiling()
            displayBatchCalls()
            displayLine("Version: ", "0.1")
            label.setText(stringBuilder)
        }
    }

    private fun displayBatchCalls() {
        displayLine(
            LABEL_UI_BATCH_RENDER_CALLS,
            (gameSessionData.stage.batch as SpriteBatch).renderCalls
        )
    }

    private fun displayGlProfiling() {
        displayLine(LABEL_GL_CALL, glProfiler.calls)
        displayLine(LABEL_DRAW_CALL, glProfiler.drawCalls)
        displayLine(LABEL_SHADER_SWITCHES, glProfiler.shaderSwitches)
        val valueWithoutText = glProfiler.textureBindings - 1
        displayLine(LABEL_TEXTURE_BINDINGS, valueWithoutText)
        displayLine(LABEL_VERTEX_COUNT, glProfiler.vertexCount.total)
        glProfiler.reset()
    }

    private fun displayLine(label: String, value: Any) {
        stringBuilder.append(label)
        stringBuilder.append(value)
        stringBuilder.append('\n')
    }

    companion object {
        private const val LABEL_FPS = "FPS:"
        private const val LABEL_UI_BATCH_RENDER_CALLS = "UI batch calls:"
        private const val LABEL_GL_CALL = "Total calls:"
        private const val LABEL_DRAW_CALL = "Draw calls:"
        private const val LABEL_SHADER_SWITCHES = "Shader switches:"
        private const val LABEL_TEXTURE_BINDINGS = "Texture bindings:"
        private const val LABEL_VERTEX_COUNT = "Vertex count:"
    }
}

