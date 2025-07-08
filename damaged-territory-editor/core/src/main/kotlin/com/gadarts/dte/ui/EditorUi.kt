package com.gadarts.dte.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.InputMultiplexer
import com.badlogic.gdx.ai.msg.MessageDispatcher
import com.badlogic.gdx.assets.AssetManager
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.ButtonGroup
import com.badlogic.gdx.scenes.scene2d.ui.Stack
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.badlogic.gdx.utils.viewport.ScreenViewport
import com.gadarts.dte.ObjectFactory
import com.gadarts.dte.PlacedTile
import com.gadarts.dte.TileFactory
import com.gadarts.dte.TileLayer
import com.gadarts.dte.scene.Modes
import com.gadarts.dte.scene.SharedData
import com.gadarts.shared.GameAssetManager
import com.gadarts.shared.SharedUtils.INITIAL_INDEX_OF_TILES_MAPPING
import com.gadarts.shared.SharedUtils.tilesChars
import com.gadarts.shared.assets.map.GameMap
import com.gadarts.shared.assets.map.GameMapPlacedObject
import com.gadarts.shared.assets.map.GameMapTileLayer
import com.gadarts.shared.assets.map.TilesMapping
import com.gadarts.shared.model.ElementType
import com.google.gson.GsonBuilder
import com.kotcrab.vis.ui.VisUI
import com.kotcrab.vis.ui.widget.*
import com.kotcrab.vis.ui.widget.file.FileChooser
import com.kotcrab.vis.ui.widget.file.FileChooserListener

class EditorUi(
    private val sharedData: SharedData,
    private val tileFactory: TileFactory,
    private val objectFactory: ObjectFactory,
    dispatcher: MessageDispatcher,
    gameAssetsManager: GameAssetManager,
) {
    fun render() {
        stage.act()
        stage.draw()
    }

    private val editorPanel = EditorPanel(sharedData, dispatcher, gameAssetsManager)
    private val modesButtonGroup: ButtonGroup<VisImageButton> by lazy {
        createButtonGroup()
    }
    private val stage: Stage by lazy { Stage(ScreenViewport()) }
    private val menuBar: MenuBar by lazy {
        addMenu()
    }
    private val editorAssetManager = AssetManager()

    private fun convertLayerToString(layer: TileLayer): String {
        val tilesString = StringBuilder()

        val tiles = layer.tiles
        for (row in tiles.indices) {
            for (col in 0 until tiles[0].size) {
                val placedTile = tiles[row][col]
                val defaultTileIndex = 0
                val index = placedTile?.definition?.let { TilesMapping.tiles.indexOf(it.fileName) }
                    ?: defaultTileIndex
                tilesString.append(tilesChars[index])
            }
        }

        return tilesString.toString()
    }

    private fun loadMap(files: com.badlogic.gdx.utils.Array<FileHandle>?) {
        val file = files?.firstOrNull()
        if (file != null) {
            try {
                val json = file.readString()
                val gameMap = GsonBuilder().setPrettyPrinting().disableHtmlEscaping()
                    .create().fromJson(json, GameMap::class.java)
                clearMapData()
                inflateLayers(gameMap)
                val definitions = ElementType.entries.flatMap { it.definitions }
                gameMap.objects.map { obj ->
                    objectFactory.addObject(
                        obj.column, obj.row,
                        definitions.first { it.getName().lowercase() == obj.definition.lowercase() }
                    )
                }
                editorPanel.mapLoaded()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }


    private fun clearMapData() {
        sharedData.layers.drop(1).forEach {
            it.tiles.forEach { tileArray ->
                tileArray.forEach { tile ->
                    if (tile != null) {
                        sharedData.modelInstances.remove(
                            tile.modelInstance
                        )
                    }
                }
            }
        }
        sharedData.placedObjects.forEach {
            sharedData.modelInstances.remove(it.modelInstance)
        }
        sharedData.layers.retainAll(listOf(sharedData.layers.first()))
        sharedData.placedObjects.clear()
    }


    private fun createButtonGroup(): ButtonGroup<VisImageButton> {
        val buttonGroup = ButtonGroup<VisImageButton>()
        buttonGroup.setMinCheckCount(1)
        buttonGroup.setMaxCheckCount(1)
        buttonGroup.setUncheckLast(true)
        return buttonGroup
    }

    private fun inflateLayers(gameMap: GameMap) {
        gameMap.layers.mapIndexed { i, layer ->
            inflateLayer(gameMap, layer, i)
        }

    }

    private fun inflateLayer(
        gameMap: GameMap,
        layer: GameMapTileLayer,
        i: Int
    ) {
        val tiles = Array<Array<PlacedTile?>>(
            gameMap.depth
        ) { Array(gameMap.width) { null } }
        layer.tiles.mapIndexed { j, tile ->
            val index = tile.code - INITIAL_INDEX_OF_TILES_MAPPING
            if (index > 0) {
                TilesMapping.tiles[index].let { textureName ->
                    val x = j % gameMap.width
                    val z = j / gameMap.width
                    tiles[z][x] = tileFactory.createTile(textureName, x, z, i + 1)
                }
            }
        }
        val bitMap = Array(gameMap.depth) { Array(gameMap.width) { 0 } }
        tiles.forEachIndexed { z, row ->
            row.forEachIndexed { x, placedTile ->
                if (placedTile != null && placedTile.definition.surroundedTile) {
                    bitMap[z][x] = 1
                }
            }
        }
        TileLayer(
            name = layer.name,
            tiles = tiles,
            bitMap = bitMap
        ).also {
            sharedData.layers.add(it)
        }
    }

    private fun showLoadMapDialog() {
        val chooser = FileChooser(FileChooser.Mode.OPEN)
        chooser.setSelectionMode(FileChooser.SelectionMode.FILES)
        chooser.setDirectory(Gdx.files.local("maps"))
        chooser.setListener(object : FileChooserListener {
            override fun selected(files: com.badlogic.gdx.utils.Array<FileHandle>?) {
                loadMap(files)
                invokeTilesModeButtonClick()
            }

            override fun canceled() {
                invokeTilesModeButtonClick()
            }
        })
        stage.addActor(chooser.fadeIn())
    }

    private fun invokeTilesModeButtonClick() {
        val event = InputEvent().apply {
            type = InputEvent.Type.touchDown
            stageX = 0f
            stageY = 0f
            button = 0
        }
        val first = modesButtonGroup.buttons[0]
        first.fire(event)

        val eventUp = InputEvent().apply {
            type = InputEvent.Type.touchUp
            stageX = 0f
            stageY = 0f
            button = 0
        }
        first.fire(eventUp)
    }

    private fun addSaveButton(buttonBar: MenuBar) {
        addButton(
            IconsTextures.ICON_FILE_SAVE.getFileName(), object : ClickListener() {
                override fun clicked(event: InputEvent?, x: Float, y: Float) {
                    super.clicked(event, x, y)
                    val gameMapTileLayers = sharedData.layers.drop(1).map { layer ->
                        val tilesString = convertLayerToString(layer)
                        GameMapTileLayer(name = layer.name, tiles = tilesString)
                    }
                    val gameMapPlacedObjects = sharedData.placedObjects.map { obj ->
                        val definition = obj.definition
                        GameMapPlacedObject(
                            definition = definition.getName(),
                            type = definition.getType(),
                            row = obj.row,
                            column = obj.column
                        )
                    }
                    val firstLayerTiles = sharedData.layers[0].tiles
                    val gameMap = GameMap(
                        layers = gameMapTileLayers, objects = gameMapPlacedObjects,
                        width = firstLayerTiles[0].size, depth = firstLayerTiles.size
                    )
                    val json = GsonBuilder().setPrettyPrinting().disableHtmlEscaping()
                        .create().toJson(gameMap)

                    val chooser = FileChooser(FileChooser.Mode.SAVE)
                    chooser.setDirectory(Gdx.files.local("maps"))
                    chooser.setSelectionMode(FileChooser.SelectionMode.FILES)

                    chooser.setListener(object : FileChooserListener {

                        override fun selected(files: com.badlogic.gdx.utils.Array<FileHandle>?) {
                            val file = files?.firstOrNull() ?: return
                            try {
                                file.writeString(json, false)
                                invokeTilesModeButtonClick()
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }

                        }

                        override fun canceled() {
                            invokeTilesModeButtonClick()
                        }
                    })

                    stage.addActor(chooser.fadeIn())
                }
            }, buttonBar.table
        )
    }


    private fun addModeButton(
        buttonGroup: ButtonGroup<VisImageButton>,
        buttonBar: MenuBar,
        mode: Modes
    ) {
        addBarRadioButton(
            buttonGroup,
            object : ClickListener() {
                override fun clicked(event: InputEvent?, x: Float, y: Float) {
                    super.clicked(event, x, y)
                    if (sharedData.selectedMode != mode) {
                        sharedData.selectedMode = mode
                        editorPanel.modeButtonClicked(mode)

                    }

                }
            },
            mode.icon.getFileName(), buttonBar.table, mode.name
        )
    }

    private fun addButton(
        iconFileName: String,
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
            TextureRegionDrawable(
                editorAssetManager.get(
                    iconFileName.lowercase(),
                    Texture::class.java
                )
            ),
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
    }

    private fun getIconAsDrawable(icon: IconsTextures) = TextureRegionDrawable(
        editorAssetManager.get(
            icon.getFileName().lowercase(),
            Texture::class.java
        )
    )

    private fun addFileButtons(buttonBar: MenuBar) {
        addLoadButton(buttonBar)
        addSaveButton(buttonBar)
    }

    private fun addLoadButton(buttonBar: MenuBar) {
        addButton(
            IconsTextures.ICON_FILE_LOAD.getFileName(), object : ClickListener() {
                override fun clicked(event: InputEvent?, x: Float, y: Float) {
                    showLoadMapDialog()

                }
            }, buttonBar.table
        )
    }

    private fun addBarRadioButton(
        buttonGroup: ButtonGroup<VisImageButton>,
        clickListener: ClickListener,
        iconFileName: String,
        table: Table,
        name: String,
    ) {
        val imageButton = addButton(iconFileName, clickListener, table)
        imageButton.name = name
        buttonGroup.add(imageButton)
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

    fun initialize() {
        VisUI.load()
        val inputMultiplexer = Gdx.input.inputProcessor as InputMultiplexer
        inputMultiplexer.addProcessor(0, stage)
        loadEditorAssets()
        val buttonBar = MenuBar()
        addFileButtons(buttonBar)
        buttonBar.table.add(Separator("vertical")).width(10F).fillY().expandY()
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
        editorPanel.initialize(root)
        root.pack()
    }

}
