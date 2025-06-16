package com.gadarts.dte

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.InputMultiplexer
import com.badlogic.gdx.ai.msg.MessageDispatcher
import com.badlogic.gdx.assets.AssetManager
import com.badlogic.gdx.files.FileHandle
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
import com.gadarts.dte.ui.*
import com.gadarts.shared.GameAssetManager
import com.gadarts.shared.SharedUtils.INITIAL_INDEX_OF_TILES_MAPPING
import com.gadarts.shared.SharedUtils.tilesChars
import com.gadarts.shared.assets.definitions.external.TextureDefinition
import com.gadarts.shared.assets.map.MapFile
import com.gadarts.shared.assets.map.MapFileLayer
import com.gadarts.shared.assets.map.MapFileObject
import com.gadarts.shared.assets.map.TilesMapping
import com.gadarts.shared.model.ElementType
import com.gadarts.shared.model.definitions.AmbDefinition
import com.google.gson.GsonBuilder
import com.kotcrab.vis.ui.VisUI
import com.kotcrab.vis.ui.widget.*
import com.kotcrab.vis.ui.widget.file.FileChooser
import com.kotcrab.vis.ui.widget.file.FileChooserListener

class DamagedTerritoryEditor(private val dispatcher: MessageDispatcher) : ApplicationAdapter() {
    private val layersListDisplay: SelectableLayerList by lazy {
        SelectableLayerList(VisUI.getSkin()).apply {
            initializeSelectableList(this, sharedData.layers.toTypedArray())
        }
    }
    private val gson = GsonBuilder().setPrettyPrinting().disableHtmlEscaping()
        .create()
    private val leftSidePanel by lazy { VisTable() }
    private val tilesModePanel by lazy { VisTable() }
    private val objectsModePanel by lazy { VisTable() }
    private var selectedMode: Modes = Modes.TILES
    private val modesButtonGroup: ButtonGroup<VisImageButton> by lazy {
        createButtonGroup()
    }
    private val stage: Stage by lazy { Stage(ScreenViewport()) }
    private lateinit var menuBar: MenuBar
    private val sharedData = SharedData()
    private val gameAssetsManager: GameAssetManager by lazy { GameAssetManager() }
    private val tileFactory = TileFactory(sharedData, gameAssetsManager)
    private val objectFactory = ObjectFactory(sharedData, gameAssetsManager)
    private val sceneRenderer: SceneRenderer by lazy {
        SceneRenderer(
            sharedData,
            gameAssetsManager,
            dispatcher,
            tileFactory,
            objectFactory
        )
    }
    private val editorAssetManager = AssetManager()
    private val tileButtonGroup = ButtonGroup<CatalogTileButton>()

    override fun create() {
        gameAssetsManager.loadAssets()
        Gdx.input.inputProcessor = InputMultiplexer(stage)
        sceneRenderer.initialize()
        VisUI.load()
        loadEditorAssets()
        menuBar = addMenu()
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
        addLeftSidePanel(root)
        root.pack()
    }

    fun convertLayerToString(layer: TileLayer): String {
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

    private fun loadMap(files: com.badlogic.gdx.utils.Array<FileHandle>?) {
        val file = files?.firstOrNull()
        if (file != null) {
            try {
                val json = file.readString()
                val mapFile = gson.fromJson(json, MapFile::class.java)
                clearMapData()
                inflateLayers(mapFile)
                mapFile.objects.map { obj ->
                    objectFactory.addObject(
                        obj.column, obj.row,
                        AmbDefinition.entries.first { it.name == obj.definition }
                    )
                }
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

    private fun addSaveButton(buttonBar: MenuBar) {
        addButton(
            IconsTextures.ICON_FILE_SAVE.getFileName(), object : ClickListener() {
                override fun clicked(event: InputEvent?, x: Float, y: Float) {
                    super.clicked(event, x, y)
                    val mapFileLayers = sharedData.layers.drop(1).map { layer ->
                        val tilesString = convertLayerToString(layer)
                        MapFileLayer(name = layer.name, tiles = tilesString)
                    }
                    val mapFileObjects = sharedData.placedObjects.map { obj ->
                        MapFileObject(
                            definition = obj.definition.name,
                            type = ElementType.AMB,
                            row = obj.row,
                            column = obj.column
                        )
                    }
                    val firstLayerTiles = sharedData.layers[0].tiles
                    val mapFile = MapFile(
                        layers = mapFileLayers, objects = mapFileObjects,
                        width = firstLayerTiles[0].size, depth = firstLayerTiles.size
                    )
                    val json = gson.toJson(mapFile)

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

    private fun addLeftSidePanel(root: VisTable) {
        leftSidePanel.background = VisUI.getSkin().getDrawable("window-bg")
        addLayersListDisplay(tilesModePanel)
        addTilesCatalog(tilesModePanel)
        leftSidePanel.add(tilesModePanel)
        leftSidePanel.pad(10F)
        leftSidePanel.align(Align.top)
        root.add(leftSidePanel).left().expandY().fillY().width(250F).row()
        createObjectsModesPanel(objectsModePanel)
    }

    private fun createObjectsModesPanel(objectsModePanel: VisTable) {
        val objectsListTable = VisTable()
        objectsListTable.background = VisUI.getSkin().getDrawable("window-bg")
        val list = SelectableList<AmbDefinition>(VisUI.getSkin()).apply {
            initializeSelectableList(this, AmbDefinition.entries.toTypedArray())
        }
        list.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent?, actor: Actor?) {
                sharedData.selectedObject = AmbDefinition.entries[list.selectedIndex]
                dispatcher.dispatchMessage(EditorEvents.OBJECT_SELECTED.ordinal)
            }
        })
        addHeaderToSelectableList(objectsListTable, "Objects")
        addSelectableListScrollPane(list, objectsListTable)
        objectsModePanel.add(objectsListTable)
    }

    private fun <T> initializeSelectableList(
        selectableList: SelectableList<T>,
        entries: Array<T>
    ) {
        selectableList.setItems(*entries)
        selectableList.style.background = VisUI.getSkin().getDrawable("window-bg")
        selectableList.selectedIndex = 1
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

    private fun addLayersListDisplay(parentPanel: VisTable) {
        val layersTable = VisTable()
        layersTable.background = VisUI.getSkin().getDrawable("window-bg")
        layersListDisplay.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent?, actor: Actor?) {
                sharedData.selectedLayerIndex = layersListDisplay.selectedIndex
                dispatcher.dispatchMessage(EditorEvents.LAYER_SELECTED.ordinal)
            }
        })
        addHeaderToSelectableList(layersTable, "Layers")
        addSelectableListScrollPane(layersListDisplay, layersTable)
        layersTable.add(addLayerButton("+", layersListDisplay)).expandX().fillX().pad(10f)
        layersTable.add(addLayerButton("-", layersListDisplay)).expandX().fillX().pad(10f).row()
        parentPanel.add(layersTable).row()
    }

    private fun addHeaderToSelectableList(selectableListTable: VisTable, header: String) {
        val layersHeader = VisLabel(header)
        layersHeader.setAlignment(Align.center)
        selectableListTable.add(layersHeader).colspan(2).center().expandX().fillX().row()
    }

    private fun <T> addSelectableListScrollPane(list: SelectableList<T>, panel: VisTable): ScrollPane {
        val scrollPane = ScrollPane(list, VisUI.getSkin())
        scrollPane.setFadeScrollBars(false)
        scrollPane.setScrollingDisabled(true, false)
        panel.add(scrollPane).colspan(2).width(200F).height(200F).top().expandX().fillX().row()
        return scrollPane
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
                override fun clicked(event: InputEvent?, x: Float, y: Float) {
                    super.clicked(event, x, y)
                    if (sharedData.selectedMode != mode) {
                        sharedData.selectedMode = mode
                        if (mode == Modes.TILES) {
                            objectsModePanel.remove()
                            leftSidePanel.add(tilesModePanel)
                        } else {
                            tilesModePanel.remove()
                            leftSidePanel.add(objectsModePanel)
                        }
                    }
                }
            },
            mode.icon.getFileName(), buttonBar.table, mode.name
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

    private fun createButtonGroup(): ButtonGroup<VisImageButton> {
        val buttonGroup = ButtonGroup<VisImageButton>()
        buttonGroup.setMinCheckCount(1)
        buttonGroup.setMaxCheckCount(1)
        buttonGroup.setUncheckLast(true)
        return buttonGroup
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

    private fun inflateLayers(mapFile: MapFile) {
        mapFile.layers.mapIndexed { i, layer ->
            inflateLayer(mapFile, layer, i)
        }
        layersListDisplay.setItems(
            *sharedData.layers.toTypedArray()
        )
    }

    private fun inflateLayer(
        mapFile: MapFile,
        layer: MapFileLayer,
        i: Int
    ) {
        val tiles = Array<Array<PlacedTile?>>(
            mapFile.depth
        ) { Array(mapFile.width) { null } }
        layer.tiles.mapIndexed { j, tile ->
            val index = tile.code - INITIAL_INDEX_OF_TILES_MAPPING
            if (index > 0) {
                TilesMapping.tiles[index].let { textureName ->
                    val x = j % mapFile.width
                    val z = j / mapFile.width
                    tiles[z][x] = tileFactory.createTile(textureName, x, z, i + 1)
                }
            }
        }
        val bitMap = Array(mapFile.depth) { Array(mapFile.width) { 0 } }
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

}
