package com.gadarts.dte

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.InputMultiplexer
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Stack
import com.badlogic.gdx.utils.ScreenUtils
import com.badlogic.gdx.utils.viewport.ScreenViewport
import com.kotcrab.vis.ui.VisUI
import com.kotcrab.vis.ui.widget.Menu
import com.kotcrab.vis.ui.widget.MenuBar
import com.kotcrab.vis.ui.widget.MenuItem
import com.kotcrab.vis.ui.widget.VisTable

class DamagedTerritoryEditor : ApplicationAdapter() {
    private val stage: Stage by lazy { Stage(ScreenViewport()) }
    private lateinit var menuBar: MenuBar
    private val sceneRenderer: SceneRenderer by lazy { SceneRenderer() }

    override fun create() {
        Gdx.input.inputProcessor = InputMultiplexer(stage)
        VisUI.load()
        menuBar = addMenu()
        val root = VisTable()
        root.setFillParent(true)
        root.top()
        stage.addActor(root)
        val stack = Stack()
        stack.setFillParent(true)
        root.add(menuBar.table).fill().expandX()
        root.pack()
        sceneRenderer.init()
    }

    private fun addMenu(): MenuBar {
        val menuBar = MenuBar()
        val fileMenu = Menu("File")
        val newItem = MenuItem("New")
        val saveItem = MenuItem("Save")
        val openItem = MenuItem("Open")
        val exitItem = MenuItem("Exit")
        exitItem.setShortcut("Ctrl+Q")
        fileMenu.addItem(newItem)
        fileMenu.addItem(saveItem)
        fileMenu.addItem(openItem)
        fileMenu.addSeparator()
        fileMenu.addItem(exitItem)
        menuBar.addMenu(fileMenu)
        return menuBar
    }

    override fun render() {
        super.render()
        Gdx.gl.glViewport(
            0,
            0,
            Gdx.graphics.backBufferWidth,
            Gdx.graphics.backBufferHeight
        )
        ScreenUtils.clear(Color.BLACK, true)
        sceneRenderer.render()
        stage.act()
        stage.draw()
    }

}
