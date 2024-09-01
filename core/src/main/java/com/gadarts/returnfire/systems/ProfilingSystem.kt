package com.gadarts.returnfire.systems

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.profiling.GLProfiler
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.gadarts.returnfire.GameDebugSettings
import com.gadarts.returnfire.Managers
import com.gadarts.returnfire.systems.data.GameSessionData
import com.gadarts.returnfire.systems.events.SystemEvents

class ProfilingSystem : GameEntitySystem() {

    private val stringBuilder: StringBuilder = StringBuilder()
    private val glProfiler: GLProfiler by lazy { GLProfiler(Gdx.graphics) }
    private val label: Label by lazy {
        val font = BitmapFont()
        font.data.setScale(2f)
        val style = Label.LabelStyle(font, Color.WHITE)
        Label(stringBuilder, style)
    }

    override val subscribedEvents: Map<SystemEvents, HandlerOnEvent> = emptyMap()
    override fun resume(delta: Long) {

    }

    override fun dispose() {
        gameSessionData.gameSessionDataHud.stage.dispose()
    }

    override fun initialize(gameSessionData: GameSessionData, managers: Managers) {
        super.initialize(gameSessionData, managers)
        setGlProfiler()
        addLabel()
    }

    private fun addLabel() {
        label.setPosition(0f, (Gdx.graphics.height - 175).toFloat())
        gameSessionData.gameSessionDataHud.stage.addActor(label)
        label.zIndex = 0
    }

    private fun setGlProfiler() {
        if (GameDebugSettings.SHOW_GL_PROFILING) {
            @Suppress("GDXKotlinProfilingCode")
            glProfiler.enable()
        }
    }

    override fun update(delta: Float) {
        if (glProfiler.isEnabled) {
            stringBuilder.setLength(0)
            displayLine(LABEL_FPS, Gdx.graphics.framesPerSecond)
            displayGlProfiling()
            displayBatchCalls()
            displayLine("Version: ", "0.5")
            displayLine("Ground blast pool:", "${gameSessionData.groundBlastPool.free}")
            gameSessionData.pools.gameModelInstancePools.forEach { pair ->
                displayLine("${pair.key} pool:", "${pair.value.free}")
            }
            gameSessionData.pools.particleEffectsPools.pools.forEach { pair ->
                displayLine("${pair.key} pool:", "${pair.value.free}")
            }
            label.setText(stringBuilder)
        }
    }

    private fun displayBatchCalls() {
        displayLine(
            LABEL_UI_BATCH_RENDER_CALLS,
            (gameSessionData.gameSessionDataHud.stage.batch as SpriteBatch).renderCalls
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

