package com.gadarts.returnfire.systems

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.profiling.GLProfiler
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.gadarts.returnfire.DamagedTerritory
import com.gadarts.returnfire.GameDebugSettings
import com.gadarts.returnfire.managers.GamePlayManagers
import com.gadarts.returnfire.systems.data.GameSessionData
import com.gadarts.returnfire.systems.events.SystemEvents

class ProfilingSystem(gamePlayManagers: GamePlayManagers) : GameEntitySystem(gamePlayManagers) {

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
    }

    override fun initialize(gameSessionData: GameSessionData, gamePlayManagers: GamePlayManagers) {
        super.initialize(gameSessionData, gamePlayManagers)
        setGlProfiler()
        addLabel()
    }

    override fun update(delta: Float) {
        if (GameDebugSettings.ENABLE_PROFILER && glProfiler.isEnabled) {
            stringBuilder.setLength(0)
            displayLine(LABEL_FPS, Gdx.graphics.framesPerSecond)
            displayHeapSize()
            displayGlProfiling()
            displayBatchCalls()
            displayLine("Version: ", DamagedTerritory.VERSION)
            if (GameDebugSettings.SHOW_OBJECT_POOL_PROFILING) {
                displayLine("Ground blast pool:", "${gameSessionData.gamePlayData.pools.groundBlastPool.free}")
                gameSessionData.gamePlayData.pools.gameModelInstancePools.forEach { pair ->
                    displayLine("GameModelInstance ${pair.key} pool:", "${pair.value.free}")
                }
                gameSessionData.gamePlayData.pools.particleEffectsPools.pools.forEach { pair ->
                    displayLine("ParticleEffect ${pair.key} pool:", "${pair.value.free}")
                }
                gameSessionData.gamePlayData.pools.rigidBodyPools.pools.forEach { pair ->
                    displayLine("RigidBody: ${pair.key} pool:", "${pair.value.free}")
                }
            }
            displayLine("Total rendered bullet holes: ", gameSessionData.profilingData.holesRendered)
            label.setText(stringBuilder)
        }
    }

    private fun addLabel() {
        label.setPosition(0f, (Gdx.graphics.height - 175).toFloat())
        gameSessionData.hudData.stage.addActor(label)
        label.zIndex = 0
    }

    private fun setGlProfiler() {
        if (GameDebugSettings.SHOW_GL_PROFILING) {
            @Suppress("GDXKotlinProfilingCode")
            glProfiler.enable()
        }
    }

    private fun displayHeapSize() {
        if (GameDebugSettings.SHOW_HEAP_SIZE) {
            displayLine(
                "Java heap usage: ",
                Gdx.app.javaHeap / (1024L * 1024L),
                false
            ).append("MB\n")
            displayLine(
                "Native heap usage: ",
                Gdx.app.nativeHeap / (1024L * 1024L),
                false
            ).append("MB\n")
        }
    }

    private fun displayBatchCalls() {
        displayLine(
            LABEL_UI_BATCH_RENDER_CALLS,
            (gameSessionData.hudData.stage.batch as SpriteBatch).renderCalls
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

    private fun displayLine(label: String, value: Any, newline: Boolean = true): StringBuilder {
        stringBuilder.append(label)
        stringBuilder.append(value)
        if (newline) {
            stringBuilder.append('\n')
        }
        return stringBuilder
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

