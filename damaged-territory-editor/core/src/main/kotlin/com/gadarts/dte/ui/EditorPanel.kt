package com.gadarts.dte.ui

import com.badlogic.gdx.ai.msg.MessageDispatcher
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.ui.*
import com.badlogic.gdx.scenes.scene2d.ui.List
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.Array
import com.gadarts.dte.EditorEvents
import com.gadarts.dte.TileLayer
import com.gadarts.dte.scene.Modes
import com.gadarts.dte.scene.SharedData
import com.gadarts.shared.GameAssetManager
import com.gadarts.shared.assets.definitions.external.TextureDefinition
import com.gadarts.shared.model.ElementType
import com.gadarts.shared.model.definitions.ElementDefinition
import com.kotcrab.vis.ui.VisUI
import com.kotcrab.vis.ui.widget.VisLabel
import com.kotcrab.vis.ui.widget.VisTable

class EditorPanel(
    private val sharedData: SharedData,
    private val dispatcher: MessageDispatcher,
    private val gameAssetsManager: GameAssetManager
) {
    private val objectList: SelectableList<ElementDefinition> by lazy {
        val entries = ElementType.entries.flatMap { it.definitions }.filter { it.isPlaceable() }
        SelectableList<ElementDefinition>(VisUI.getSkin()).apply {
            val array = Array<ElementDefinition>()
            array.addAll(*entries.filter { it.isPlaceable() }.toTypedArray())
            setItems(array)
            style.background = VisUI.getSkin().getDrawable("window-bg")
            selectedIndex = 1
        }
    }
    private val leftSidePanel by lazy { VisTable() }
    private val tilesModePanel by lazy { VisTable() }
    private val objectsModePanel by lazy { VisTable() }
    private val tileButtonGroup = ButtonGroup<CatalogTileButton>()
    private val layersListDisplay: SelectableLayerList by lazy {
        SelectableLayerList(VisUI.getSkin()).apply {
            val entries = sharedData.layers
            val array = Array<TileLayer>()
            array.addAll(*entries.toTypedArray())
            setItems(array)
            style.background = VisUI.getSkin().getDrawable("window-bg")
            selectedIndex = 1
        }
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


    fun modeButtonClicked(mode: Modes) {
        if (mode == Modes.TILES) {
            objectsModePanel.remove()
            leftSidePanel.add(tilesModePanel)
            sharedData.selectedLayerIndex = 1
            layerSelected()
        } else {
            tilesModePanel.remove()
            leftSidePanel.add(objectsModePanel)
            objectList.selectedIndex = 0
        }
        dispatcher.dispatchMessage(EditorEvents.MODE_CHANGED.ordinal, mode)
    }

    fun initialize(root: VisTable) {
        addLeftSidePanel(root)
    }

    private fun createObjectsModesPanel(objectsModePanel: VisTable) {
        val objectsListTable = VisTable()
        objectsListTable.background = VisUI.getSkin().getDrawable("window-bg")
        objectList.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent?, actor: Actor?) {
                sharedData.selectedObject = objectList.items[objectList.selectedIndex]
                dispatcher.dispatchMessage(EditorEvents.OBJECT_SELECTED.ordinal)
            }
        })
        addHeaderToSelectableList(objectsListTable, "Objects")
        addSelectableListScrollPane(objectList, objectsListTable)
        objectsModePanel.add(objectsListTable)
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
                layerSelected()
            }
        })
        addHeaderToSelectableList(layersTable, "Layers")
        addSelectableListScrollPane(layersListDisplay, layersTable)
        layersTable.add(addLayerButton("+", layersListDisplay)).expandX().fillX().pad(10f)
        layersTable.add(addLayerButton("-", layersListDisplay)).expandX().fillX().pad(10f).row()
        parentPanel.add(layersTable).row()
    }

    private fun layerSelected() {
        sharedData.selectedLayerIndex = layersListDisplay.selectedIndex
        dispatcher.dispatchMessage(EditorEvents.LAYER_SELECTED.ordinal)
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
        leftSidePanel.add(scrollPane).width(250F).fill()
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

    fun mapLoaded() {
        layersListDisplay.setItems(
            *sharedData.layers.toTypedArray()
        )
        layersListDisplay.selectedIndex = 1
    }

}
