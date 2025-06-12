package com.gadarts.dte

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.InputMultiplexer
import com.badlogic.gdx.ai.msg.MessageDispatcher
import com.badlogic.gdx.assets.AssetManager
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.*
import com.badlogic.gdx.scenes.scene2d.ui.List
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.ScreenUtils
import com.badlogic.gdx.utils.viewport.ScreenViewport
import com.gadarts.dte.scene.SceneRenderer
import com.gadarts.dte.scene.SharedData
import com.gadarts.dte.ui.CatalogTileButton
import com.gadarts.dte.ui.IconsTextures
import com.gadarts.dte.ui.Modes
import com.gadarts.dte.ui.TileLayerList
import com.gadarts.shared.GameAssetManager
import com.gadarts.shared.assets.definitions.external.TextureDefinition
import com.kotcrab.vis.ui.VisUI
import com.kotcrab.vis.ui.widget.*

class DamagedTerritoryEditor : ApplicationAdapter() {
    private var selectedMode: Modes = Modes.TILES
    private val modesButtonGroup: ButtonGroup<VisImageButton> by lazy {
        createButtonGroup()
    }
    private val stage: Stage by lazy { Stage(ScreenViewport()) }
    private lateinit var menuBar: MenuBar
    private val sharedData = SharedData()
    private val dispatcher = MessageDispatcher()
    private val sceneRenderer: SceneRenderer by lazy { SceneRenderer(sharedData, gameAssetsManager, dispatcher) }
    private val editorAssetManager = AssetManager()
    private val gameAssetsManager: GameAssetManager by lazy { GameAssetManager() }
    private val tileButtonGroup = ButtonGroup<CatalogTileButton>()

    override fun create() {
        gameAssetsManager.loadAssets()
        Gdx.input.inputProcessor = InputMultiplexer(stage)
        sceneRenderer.initialize()
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
        leftSidePanel.background = VisUI.getSkin().getDrawable("window-bg")
        addLayersListDisplay(leftSidePanel)
        addTilesCatalog(leftSidePanel)
        leftSidePanel.pad(10F)
        leftSidePanel.align(Align.top)
        root.add(leftSidePanel).left().expandY().fillY().width(250F).row()
    }

    private fun addTilesCatalog(leftSidePanel: VisTable) {
        val catalogTable = Table()
        val scrollPane = ScrollPane(catalogTable, VisUI.getSkin())
        scrollPane.setFadeScrollBars(false)
        tileButtonGroup.setMaxCheckCount(1)
        tileButtonGroup.setMinCheckCount(1)
        val textureDefinitions = gameAssetsManager.getTexturesDefinitions().definitions.filter {
            it.value.folder.split(
                "/"
            ).first() == "ground"
        }.values
        textureDefinitions.forEachIndexed { i, tileType ->
            addTileCatalogButton(tileType, catalogTable, i)
        }
        sharedData.selectedTile = textureDefinitions.first()
        leftSidePanel.add(scrollPane).size(250F)
    }

    private fun addTileCatalogButton(
        textureDefinition: TextureDefinition,
        catalogTable: Table,
        i: Int
    ) {
        val texture = gameAssetsManager.getTexture(textureDefinition.fileName.lowercase())
        val borderDrawable = TextureRegionDrawable(
            createBorderedTexture(texture)
        )
        val style = ImageButton.ImageButtonStyle().apply {
            imageUp = TextureRegionDrawable(TextureRegion(texture))
            imageChecked = borderDrawable
        }
        val button = CatalogTileButton(style)
        button.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent?, actor: Actor?) {
                if (button.isChecked) {
                    sharedData.selectedTile = textureDefinition
                }
            }
        })
        tileButtonGroup.add(button)
        catalogTable.add(button).size(texture.width.toFloat(), texture.height.toFloat()).pad(5f)
        if ((i + 1) % 2 == 0) catalogTable.row()
    }

    private fun createBorderedTexture(baseTexture: Texture): Texture {
        val textureData = baseTexture.textureData
        if (!textureData.isPrepared) {
            textureData.prepare()
        }
        val originalPixmap = textureData.consumePixmap()
        val pixmap = Pixmap(originalPixmap.width, originalPixmap.height, Pixmap.Format.RGBA8888)
        pixmap.drawPixmap(originalPixmap, 0, 0)
        if (textureData.disposePixmap()) {
            originalPixmap.dispose()
        }
        pixmap.setColor(Color.RED)
        val w = pixmap.width
        val h = pixmap.height
        repeat(6) { i ->
            pixmap.drawRectangle(i, i, w - 2 * i, h - 2 * i)
        }
        val newTexture = Texture(pixmap)
        pixmap.dispose()

        return newTexture
    }

    private fun addLayersListDisplay(leftSidePanel: VisTable) {
        val layersTable = VisTable()
        layersTable.background = VisUI.getSkin().getDrawable("window-bg")
        val list = TileLayerList(VisUI.getSkin()).apply {
            setItems(*sharedData.layers.toTypedArray())
            style.background = VisUI.getSkin().getDrawable("window-bg")
        }
        list.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent?, actor: Actor?) {
                sharedData.selectedLayerIndex = list.selectedIndex
                dispatcher.dispatchMessage(EditorEvents.LAYER_SELECTED.ordinal)
            }
        })
        list.selectedIndex = 1
        val scrollPane = ScrollPane(list, VisUI.getSkin())
        scrollPane.setFadeScrollBars(false)
        scrollPane.setScrollingDisabled(true, false)
        val layersHeader = VisLabel("Layers")
        layersHeader.setAlignment(Align.center)
        layersTable.add(layersHeader).colspan(2).center().expandX().fillX().row()
        layersTable.add(scrollPane).colspan(2).height(100F).top().expandX().fillX().row()
        layersTable.add(addLayerButton("+", list)).expandX().fillX().pad(10f)
        layersTable.add(addLayerButton("-", list)).expandX().fillX().pad(10f).row()
        leftSidePanel.add(layersTable).row()
    }

    override fun dispose() {
        super.dispose()
        sharedData.dispose()
    }

    private fun addLayerButton(label: String, list: List<TileLayer>): TextButton {
        val addLayer = TextButton(label, VisUI.getSkin())
        addLayer.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                val layers = sharedData.layers
                val newLayerName = "Layer ${layers.size + 1}"
                dispatcher.dispatchMessage(
                    EditorEvents.LAYER_ADDED.ordinal,
                    newLayerName
                )
                list.setItems(*layers.toTypedArray())
                list.items.last().let { lastLayer ->
                    list.selectedIndex = layers.indexOf(lastLayer)
                    sharedData.selectedLayerIndex = list.selectedIndex
                }
            }
        })
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
