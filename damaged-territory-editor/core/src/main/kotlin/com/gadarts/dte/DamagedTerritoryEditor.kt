package com.gadarts.dte

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.InputMultiplexer
import com.badlogic.gdx.assets.AssetManager
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.*
import com.badlogic.gdx.scenes.scene2d.ui.List
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.ScreenUtils
import com.badlogic.gdx.utils.viewport.ScreenViewport
import com.kotcrab.vis.ui.VisUI
import com.kotcrab.vis.ui.widget.*

class DamagedTerritoryEditor : ApplicationAdapter() {
    private var selectedMode: Modes = Modes.TILES
    private val modesButtonGroup: ButtonGroup<VisImageButton> by lazy {
        createButtonGroup()
    }
    private val stage: Stage by lazy { Stage(ScreenViewport()) }
    private lateinit var menuBar: MenuBar
    private val sceneRenderer: SceneRenderer by lazy { SceneRenderer() }
    private val editorAssetManager = AssetManager()

    override fun create() {
        Gdx.input.inputProcessor = InputMultiplexer(stage)
        VisUI.load()
        loadEditorAssets()
        menuBar = addMenu()
        val buttonBar = MenuBar()
        Modes.entries.forEach {
            addModeButton(modesButtonGroup, buttonBar, it)
        }
        buttonBar.table.pack()
        val root = VisTable()
        root.setFillParent(true)
        root.top()
        stage.addActor(root)
        val stack = Stack()
        stack.setFillParent(true)
        root.add(menuBar.table).fill().expandX().row()
        root.add(buttonBar.table).fillX().expandX().row()
        addLeftSidePanel(root)
        root.pack()
    }

    private fun addLeftSidePanel(root: VisTable) {
        val leftSidePanel = VisTable()
        addLayersList(leftSidePanel)
        leftSidePanel.pad(10F)
        leftSidePanel.align(Align.top)
        root.add(leftSidePanel).left().expandY().fillY().width(200F).row()
    }

    private fun addLayersList(leftSidePanel: VisTable) {
        val layersHeader = VisLabel("Layers")
        layersHeader.setAlignment(Align.center)
        leftSidePanel.background = VisUI.getSkin().getDrawable("window-bg")
        val items = arrayOf("Item 1", "Item 2", "Item 3", "Item 4", "Item 5")
        val list = List<String>(VisUI.getSkin()).apply {
            setItems(*items)
            val style = style
            style.background = VisUI.getSkin().getDrawable("window-bg")
        }
        val scrollPane = ScrollPane(list, VisUI.getSkin())
        scrollPane.setFadeScrollBars(false)
        scrollPane.setScrollingDisabled(true, false)
        val addLayer = addLayerButton("+")
        val removeLayer = addLayerButton("-")
        leftSidePanel.add(layersHeader).colspan(2).center().expandX().fillX().row()
        leftSidePanel.add(scrollPane).colspan(2).top().expandX().fillX().row()
        leftSidePanel.add(addLayer).expandX().fillX().pad(10f)
        leftSidePanel.add(removeLayer).expandX().fillX().pad(10f).row()
    }

    private fun addLayerButton(label: String): TextButton {
        val addLayer = TextButton(label, VisUI.getSkin())
        addLayer.addListener {
            false
        }
        return addLayer
    }

    private fun addModeButton(
        buttonGroup: ButtonGroup<VisImageButton>,
        buttonBar: MenuBar,
        mode: Modes
    ) {
        addBarRadioButton(
            buttonGroup,
            object : ClickListener() {
            },
            editorAssetManager.get(mode.icon.getFileName().lowercase(), Texture::class.java), buttonBar.table, mode.name
        )
    }

    private fun addBarRadioButton(
        buttonGroup: ButtonGroup<VisImageButton>,
        clickListener: ClickListener,
        icon: Texture,
        table: Table,
        name: String,
    ) {
        val imageButton = addButton(icon, clickListener, table)
        imageButton.name = name
        buttonGroup.add(imageButton)
    }

    private fun createButtonGroup(): ButtonGroup<VisImageButton> {
        val buttonGroup = ButtonGroup<VisImageButton>()
        buttonGroup.setMinCheckCount(1)
        buttonGroup.setMaxCheckCount(1)
        buttonGroup.setUncheckLast(true)
        return buttonGroup
    }

    private fun addButton(
        icon: Texture,
        clickListener: ClickListener,
        table: Table
    ): VisImageButton {
        val up =
            TextureRegionDrawable(
                editorAssetManager.get(
                    IconsTextures.BUTTON_UP.getFileName().lowercase(),
                    Texture::class.java
                )
            )
        val style = VisImageButton.VisImageButtonStyle(
            up,
            TextureRegionDrawable(
                editorAssetManager.get(
                    IconsTextures.BUTTON_DOWN.getFileName().lowercase(),
                    Texture::class.java
                )
            ),
            TextureRegionDrawable(
                editorAssetManager.get(
                    IconsTextures.BUTTON_CHECKED.getFileName().lowercase(),
                    Texture::class.java
                )
            ),
            TextureRegionDrawable(icon),
            null, null
        )
        style.over =
            TextureRegionDrawable(
                editorAssetManager.get(
                    IconsTextures.BUTTON_OVER.getFileName().lowercase(),
                    Texture::class.java
                )
            )
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
        val editMenu = createEditMenu()
        menuBar.addMenu(fileMenu)
        menuBar.addMenu(editMenu)
        return menuBar
    }

    private fun createEditMenu(): Menu {
        val editMenu = Menu("Edit")
        val tilesMenuItem = MenuItem("Tiles Mode", getIconAsDrawable(IconsTextures.ICON_MODE_FLOOR))
        val objectsMenuItem = MenuItem("Objects Mode", getIconAsDrawable(IconsTextures.ICON_MODE_ENV_OBJECTS))
        tilesMenuItem.isChecked = true
        tilesMenuItem.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent?, actor: Actor?) {
                if (tilesMenuItem.isChecked) {
                    objectsMenuItem.isChecked = false
                    modeChanged(Modes.TILES)
                }
            }
        })
        objectsMenuItem.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent?, actor: Actor?) {
                if (objectsMenuItem.isChecked) {
                    tilesMenuItem.isChecked = false
                    modeChanged(Modes.OBJECTS)
                }
            }
        })
        editMenu.addItem(tilesMenuItem)
        editMenu.addItem(objectsMenuItem)
        return editMenu
    }

    private fun modeChanged(mode: Modes) {
        modesButtonGroup.buttons.first { it.name.equals(mode.name) }.isChecked = true
        selectedMode = mode
    }

    private fun getIconAsDrawable(icon: IconsTextures) = TextureRegionDrawable(
        editorAssetManager.get(
            icon.getFileName().lowercase(),
            Texture::class.java
        )
    )

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
