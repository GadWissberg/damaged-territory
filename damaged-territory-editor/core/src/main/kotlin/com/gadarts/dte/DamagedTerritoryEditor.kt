package com.gadarts.dte

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.InputMultiplexer
import com.badlogic.gdx.assets.AssetManager
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.ButtonGroup
import com.badlogic.gdx.scenes.scene2d.ui.Stack
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.badlogic.gdx.utils.ScreenUtils
import com.badlogic.gdx.utils.viewport.ScreenViewport
import com.kotcrab.vis.ui.VisUI
import com.kotcrab.vis.ui.widget.*

class DamagedTerritoryEditor : ApplicationAdapter() {
    private val stage: Stage by lazy { Stage(ScreenViewport()) }
    private lateinit var menuBar: MenuBar
    private val sceneRenderer: SceneRenderer by lazy { SceneRenderer() }
    private val editorAssetManager = AssetManager()

    override fun create() {
        Gdx.input.inputProcessor = InputMultiplexer(stage)
        VisUI.load()
        menuBar = addMenu()
        loadEditorAssets()
        val buttonBar = MenuBar()
        addButtonToButtonsBar(buttonBar, IconsTextures.ICON_MODE_FLOOR)
        addButtonToButtonsBar(buttonBar, IconsTextures.ICON_MODE_ENV_OBJECTS)
        buttonBar.table.add(Separator("vertical")).width(10F).fillY().expandY()
        val buttonGroup = createButtonGroup()
        Modes.entries.forEach {
            addModeButton(buttonGroup, buttonBar, it.icon.getFileName())
        }
        buttonBar.table.pack()
        val root = VisTable()
        root.setFillParent(true)
        root.top()
        stage.addActor(root)
        val stack = Stack()
        stack.setFillParent(true)
        root.add(menuBar.table).fill().expandX()
        root.add(buttonBar.table).fillX().expandX().row()
        root.pack()
    }

    private fun addModeButton(
        buttonGroup: ButtonGroup<VisImageButton>,
        buttonBar: MenuBar,
        icon: String
    ) {
        addBarRadioButton(
            buttonGroup,
            object : ClickListener() {
                override fun clicked(event: InputEvent?, x: Float, y: Float) {
                    super.clicked(event, x, y)
                }
            },
            editorAssetManager.get(icon, Texture::class.java), buttonBar.table
        )
    }

    private fun addBarRadioButton(
        buttonGroup: ButtonGroup<VisImageButton>,
        clickListener: ClickListener,
        icon: Texture,
        table: Table,
    ) {
        val imageButton = addButton(icon, clickListener, table)
        buttonGroup.add(imageButton)
    }

    private fun createButtonGroup(): ButtonGroup<VisImageButton> {
        val buttonGroup = ButtonGroup<VisImageButton>()
        buttonGroup.setMinCheckCount(1)
        buttonGroup.setMaxCheckCount(1)
        buttonGroup.setUncheckLast(true)
        return buttonGroup
    }

    private fun addButtonToButtonsBar(buttonBar: MenuBar, icon: IconsTextures) {
        addButton(
            editorAssetManager.get(icon.getFileName(), Texture::class.java),
            object : ClickListener() {
                override fun clicked(event: InputEvent?, x: Float, y: Float) {
                }
            }, buttonBar.table
        )
    }

    private fun addButton(
        icon: Texture,
        clickListener: ClickListener,
        table: Table
    ): VisImageButton {
        val up =
            TextureRegionDrawable(editorAssetManager.get(IconsTextures.BUTTON_UP.getFileName(), Texture::class.java))
        val style = VisImageButton.VisImageButtonStyle(
            up,
            TextureRegionDrawable(editorAssetManager.get(IconsTextures.BUTTON_DOWN.getFileName(), Texture::class.java)),
            TextureRegionDrawable(
                editorAssetManager.get(
                    IconsTextures.BUTTON_CHECKED.getFileName(),
                    Texture::class.java
                )
            ),
            TextureRegionDrawable(icon),
            null, null
        )
        style.over =
            TextureRegionDrawable(editorAssetManager.get(IconsTextures.BUTTON_OVER.getFileName(), Texture::class.java))
        val imageButton = VisImageButton(style)
        imageButton.addListener(clickListener)
        table.add(imageButton).pad(5F)
        return imageButton
    }

    private fun loadEditorAssets() {
        IconsTextures.entries.forEach {
            editorAssetManager.load(
                it.getFileName().lowercase(),
                Texture::class.java
            )
        }
        editorAssetManager.finishLoading()
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
