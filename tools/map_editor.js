class CellData {
    constructor() {
        this.object = null,
            this.value = 0
    }
}

const ID_MAP = "map"
const Modes = Object.freeze({
    TILES: Symbol("tiles"),
    OBJECTS: Symbol("objects")
});
const ElementsDefinitions = Object.freeze([
    "PLAYER",
    "PALM_TREE",
    "ROCK",
    "BUILDING",
    "FENCE",
    "LIGHT_POLE",
    "BARRIER",
    "CABIN",
    "CAR",
    "GUARD_HOUSE",
    "ANTENNA",
    "WATCH_TOWER",
    "BUILDING_FLAG"
]);
const Directions = Object.freeze({ east: 0, north: 90, west: 180, south: 270 });
const MAP_SIZES = Object.freeze({ small: 48, medium: 96, large: 192 });
const body = document.body;
const table = document.getElementById(ID_MAP).appendChild(document.createElement('table'));
const RADIO_GROUP_NAME_MODES = "modes"
const OPTION_MODE_OBJECTS = "option_mode_objects"
const DIV_ID_LEFT_MENU = "left_menu_div"
const CLASS_NAME_GAME_OBJECT_SELECTION = "game_object_selection";
const CLASS_NAME_GAME_OBJECT_RADIO = "game_object_radio";
const RADIO_GROUP_NAME_GAME_OBJECT_SELECTIONS = "game_object_selections";
const DIV_ID_BUTTON_SAVE = "button_save";
const DIV_ID_BUTTON_LOAD = "button_load";
const OUTPUT_FILE_NAME = 'map.json';
const SELECT_ID_DROPDOWN_MAP_SIZES = "dropdown_map_sizes";
const DIV_ID_DIRECTION = "direction";
const DIV_ID_CELL_CONTENTS = "cell_contents";
const CLASS_NAME_CELL_CONTENTS = "cellContents";

tilesMaskMapping = [];
tilesMaskMapping[0b11010000] = 'tile_beach_bottom_right'
tilesMaskMapping[0b11111000] = 'tile_beach_bottom'
tilesMaskMapping[0b01101000] = 'tile_beach_bottom_left'
tilesMaskMapping[0b11010110] = 'tile_beach_right'
tilesMaskMapping[0b01101011] = 'tile_beach_left'
tilesMaskMapping[0b00010110] = 'tile_beach_top_right'
tilesMaskMapping[0b00011111] = 'tile_beach_top'
tilesMaskMapping[0b00001011] = 'tile_beach_top_left'
tilesMaskMapping[0b11111111] = 'tile_beach'
const tiles = [
    'tile_water',
    'tile_beach_bottom_right',
    'tile_beach_bottom',
    'tile_beach_bottom_left',
    'tile_beach_right',
    'tile_beach_left',
    'tile_beach_top_right',
    'tile_beach_top',
    'tile_beach_top_left',
    'tile_beach',
]
class MapEditor {

    constructor() {
        this.resetMap();
        this.initializeModesRadioButtons();
        this.inflateElementsLeftMenu();
        fillMapSizesDropdown();
        table.style.width = this.map_size * 64 + 'px';
        table.style.height = this.map_size * 64 + 'px';
        var self = this;
        defineSaveProcess();
        defineLoadProcess();

        function fillMapSizesDropdown() {
            var dropDown = document.getElementById(SELECT_ID_DROPDOWN_MAP_SIZES);
            for (var size in MAP_SIZES) {
                var option = document.createElement("option");
                option.value = size;
                option.text = size;
                dropDown.appendChild(option);
            }
            dropDown.addEventListener("change", function () {
                self.resetMap(MAP_SIZES[dropDown.value]);
            })
        }

        function defineLoadProcess() {
            document.getElementById(DIV_ID_BUTTON_LOAD).addEventListener('click', () => {

                var input = document.createElement('input');
                input.type = 'file';
                input.addEventListener('change', e => {
                    var file = e.target.files[0];
                    if (!file) {
                        return;
                    }
                    var reader = new FileReader();
                    reader.onload = e => {
                        var contents = e.target.result;
                        var inputMapObject = JSON.parse(contents);
                        self.resetMap(inputMapObject.size);
                        inflateTiles();
                        for (var i = 0; i < inputMapObject.elements.length; i++) {
                            var element = inputMapObject.elements[i];
                            self.placeElementObject(table.rows[element.row].cells[element.col], element.definition, element.direction);
                        }

                        function inflateTiles() {
                            for (var i = 0; i < inputMapObject.tiles.length; i++) {
                                var cell = table.rows[Math.floor(i / self.map_size)].cells[i % self.map_size];
                                var placedTile = self.findTileBySymbol(inputMapObject.tiles.charAt(i));
                                cell.style.backgroundColor = placedTile.tile;
                                cell.cellData = new CellData();
                                cell.cellData.tile = placedTile;
                            }
                        }
                    };
                    reader.readAsText(file);
                }, false);
                input.click();
            });
        }

        function defineSaveProcess() {
            document.getElementById(DIV_ID_BUTTON_SAVE).addEventListener('click', e => {
                var output = {};
                var tilesString = calculateTilesMapString();
                output.tiles = tilesString;
                var elementsArray = calculateElementsArray();
                output.elements = elementsArray;
                output.size = self.map_size;
                var json = JSON.stringify(output);
                saveJsonToFile(json);

                function calculateElementsArray() {
                    var elementsArray = [];
                    for (var row in table.rows) {
                        for (var col in table.rows[row].cells) {
                            if (table.rows[row].cells[col].cellData != null) {
                                if (table.rows[row].cells[col].cellData.object != null) {
                                    deflateElementObject(elementsArray, row, col);
                                }
                            }
                        }
                    }
                    return elementsArray;

                    function deflateElementObject(elementsArray, row, col) {
                        var elementObject = {};
                        var object = table.rows[row].cells[col].cellData.object;
                        elementObject.definition = object.definition;
                        elementObject.direction = object.direction;
                        elementObject.row = parseInt(row);
                        elementObject.col = parseInt(col);
                        elementsArray.push(elementObject);
                    }
                }

                function calculateTilesMapString() {
                    var tilesString = "";
                    for (var row = 0; row < table.rows.length; row++) {
                        for (var col = 0; col < table.rows[row].cells.length; col++) {
                            var currentTile = 0;
                            var cellData = table.rows[row].cells[col].cellData;
                            if (cellData != null && cellData.selectedTile != null) {
                                let result = tiles.find(str => str === cellData.selectedTile);
                                if (result){
                                    currentTile = tiles.indexOf(result)
                                }
                            }
                            tilesString += currentTile;
                        }
                    }
                    return tilesString;
                }

                function saveJsonToFile(json) {
                    var bb = new Blob([json], { type: 'text/json' });
                    var a = document.createElement('a');
                    a.download = OUTPUT_FILE_NAME;
                    a.href = window.URL.createObjectURL(bb);
                    a.click();
                }


            });
        }
    }

    inflateElementsLeftMenu() {
        var leftMenu = document.getElementById(DIV_ID_LEFT_MENU);
        ElementsDefinitions.forEach(element => {
            var div = document.createElement("div");
            var radioButton = addRadioButtonForElement(div);
            div.className = CLASS_NAME_GAME_OBJECT_SELECTION;
            var label = document.createElement("label");
            label.for = radioButton.id;
            label.appendChild(document.createTextNode(element));
            div.appendChild(label);
            leftMenu.appendChild(div);

            function addRadioButtonForElement(div) {
                var radioButton = document.createElement("input");
                radioButton.type = "radio";
                radioButton.className = CLASS_NAME_GAME_OBJECT_RADIO;
                radioButton.name = RADIO_GROUP_NAME_GAME_OBJECT_SELECTIONS;
                radioButton.value = element;
                radioButton.id = "element_selection_" + element;
                div.appendChild(radioButton);
                return radioButton;
            }
        });
    }

    findChildTextNode(cellContents) {
        if (cellContents == null) return;
        var textNode = null
        for (var i = 0; i < cellContents.childNodes.length; i++) {
            var curNode = cellContents.childNodes[i];
            if (curNode.nodeName === "#text") {
                textNode = curNode;
                break;
            }
        }
        return textNode
    }

    placeElementInCell(cell, cellData, input, row, col) {
        var selectedMode = Modes[document.querySelector('input[name="' + RADIO_GROUP_NAME_MODES + '"]:checked').value];
        var self = this;
        var leftClick = input == "click";
        if (selectedMode == Modes.TILES) {
            this.applyTileChangeInCell(leftClick, cellData, cell, row, col);
        } else if (selectedMode == Modes.OBJECTS) {
            applyElementChangeInCell();
        }

        function applyElementChangeInCell() {
            if (leftClick) {
                var selection = document.querySelector('input[name="' + RADIO_GROUP_NAME_GAME_OBJECT_SELECTIONS + '"]:checked').value;
                self.placeElementObject(cell, selection);
            } else {
                removeElementObject(editor, cell);
            }
        }




        function removeElementObject(editor, cell) {
            var textNode = editor.getOrAddChildTextNode(cell);
            cellData.object = null;
            textNode.nodeValue = null;
            var directionDiv = document.getElementById(DIV_ID_DIRECTION + "_" + cell.closest('tr').rowIndex + "_" + cell.cellIndex);
            if (directionDiv != null) {
                directionDiv.removeChild(directionDiv.getElementsByTagName('img')[0]);
            }
        }
    }


    applyTileChangeInCell(leftClick, cellData, cell, row, col) {
        cellData.value = 1;
        applyTileOnCell(cell, 'tile_beach')
        for (let adjRow = row - 1; adjRow < row + 2; adjRow++) {
            for (let adjCol = col - 1; adjCol < col + 2; adjCol++) {
                if (adjRow >= 0 && adjRow < this.map_size && adjCol >= 0 && adjCol < this.map_size && (adjRow != row || adjCol != col)) {
                    var adjCell = table.rows[adjRow].cells[adjCol]
                    this.initializeCellData(adjCell)
                    adjCell.cellData.value = 1
                }
            }
        }
        for (let adjRow = row - 1; adjRow < row + 2; adjRow++) {
            for (let adjCol = col - 1; adjCol < col + 2; adjCol++) {
                if (adjRow >= 0 && adjRow < this.map_size && adjCol >= 0 && adjCol < this.map_size && (adjRow != row || adjCol != col)) {
                    var adjCell = table.rows[adjRow].cells[adjCol]
                    applyTileOnCell(adjCell, tilesMaskMapping[this.calculateMask(adjRow, adjCol)])
                }
            }
        }
    }

    calculateMask(row, col) {
        var mask = 0
        if (row > 0 && col > 0) {
            mask |= (fetchValueFromCell(row - 1, col - 1) << 7) & 0b10000000;
        }
        if (row > 0) {
            mask |= (fetchValueFromCell(row - 1, col) << 6) & 0b01000000;
        }
        if (row > 0 && col < this.map_size - 1) {
            mask |= (fetchValueFromCell(row - 1, col + 1) << 5) & 0b00100000;
        }
        if (col > 0) {
            mask |= (fetchValueFromCell(row, col - 1) << 4) & 0b00010000;
        }
        if (col < this.map_size - 1) {
            mask |= (fetchValueFromCell(row, col + 1) << 3) & 0b00001000;
        }
        if (row < this.map_size - 1 && col > 0) {
            mask |= (fetchValueFromCell(row + 1, col - 1) << 2) & 0b00000100;
        }
        if (row < this.map_size - 1) {
            mask |= (fetchValueFromCell(row + 1, col) << 1) & 0b00000010;
        }
        if (row < this.map_size - 1 && col < this.map_size - 1) {
            mask |= (fetchValueFromCell(row + 1, col + 1)) & 0b00000001;
        }
        return mask

        function fetchValueFromCell(row, col) {
            const cellData = table.rows[row].cells[col].cellData;
            if (cellData) {
                return cellData.value;
            } else {
                return 0
            }
        }
    }

    initializeCellData(cell) {
        if (cell.cellData == null) {
            cell.cellData = new CellData();
        }
    }

    placeElementObject(cell, selection, direction = Directions.east) {
        this.initializeCellData(cell);
        if (cell.cellData.object != null) {
            direction = (cell.cellData.object.direction + 90) % 360;
        }
        cell.cellData.object = { definition: selection, direction: direction };
        this.getOrAddChildTextNode(cell).nodeValue = selection;
        var directionDiv = updateDirection();

        function updateDirection() {
            var directionDiv = document.getElementById(DIV_ID_DIRECTION + "_" + cell.closest('tr').rowIndex + "_" + cell.cellIndex);
            if (directionDiv == null) {
                directionDiv = createArrowImage();
            }
            directionDiv.getElementsByTagName('img')[0].style.transform = "translate(-50%, -50%) rotate(" + (-1 * direction) + "deg) ";
            return directionDiv;
        }


        function createArrowImage() {
            directionDiv = document.createElement('div');
            directionDiv.id = DIV_ID_DIRECTION + "_" + cell.closest('tr').rowIndex + "_" + cell.cellIndex;
            var imageElement = document.createElement('img');
            imageElement.src = "arrow.png";
            directionDiv.appendChild(imageElement);
            return document.getElementById(DIV_ID_CELL_CONTENTS + "_" + cell.closest('tr').rowIndex + "_" + cell.cellIndex).appendChild(directionDiv);
        }
    }

    onCellLeftClicked(row, col) {
        var cell = table.rows[row].cells[col];
        this.initializeCellData(cell);
        var cellData = cell.cellData;
        this.placeElementInCell(cell, cellData, "click", row, col);
    }

    onCellRightClicked(row, col) {
        var cell = table.rows[row].cells[col]
        this.initializeCellData(cell);
        var cellData = cell.cellData;
        var selectedMode = Modes[document.querySelector('input[name="' + RADIO_GROUP_NAME_MODES + '"]:checked').value];
        this.placeElementInCell(cell, cellData, selectedMode, "contextmenu");
    }

    getOrAddChildTextNode(cell) {
        var cellContentsId = DIV_ID_CELL_CONTENTS + "_" + cell.closest('tr').rowIndex + "_" + cell.cellIndex;
        var textNode = this.findChildTextNode(document.getElementById(cellContentsId));
        if (textNode == null) {
            createCellContents();
        }
        return textNode

        function createCellContents() {
            var cellContents = document.createElement('div');
            cell.appendChild(cellContents);
            cellContents.id = cellContentsId;
            textNode = document.createTextNode("!");
            cellContents.appendChild(textNode);
            cellContents.className = CLASS_NAME_CELL_CONTENTS;
        }
    }

    resetMap(map_size = MAP_SIZES.small) {
        table.innerHTML = "";
        this.map_size = map_size;
        document.getElementById(SELECT_ID_DROPDOWN_MAP_SIZES).value = findMapSizeDefinitionByValue(this.map_size);
        for (let i = 0; i < this.map_size; i++) {
            const tr = table.insertRow();
            for (let j = 0; j < this.map_size; j++) {
                const td = tr.insertCell();
                applyTileOnCell(td, 'tile_water');
                td.classList.add('cell');
                td.addEventListener('click', e => {
                    this.onCellLeftClicked(i, j);
                })
                td.addEventListener('contextmenu', e => {
                    e.preventDefault();
                    this.onCellRightClicked(i, j);
                    return false;
                }, false)
            }
        }

        function findMapSizeDefinitionByValue(value) {
            var result = null;
            for (const [key, sizeValue] of Object.entries(MAP_SIZES)) {
                if (sizeValue == value) {
                    result = key;
                }
            }
            return result;
        }
    }


    initializeModesRadioButtons() {
        const leftMenuDiv = document.getElementById(DIV_ID_LEFT_MENU).style;

        function onRadioButtonChecked(event) {
            if (event.target.id == OPTION_MODE_OBJECTS) {
                leftMenuDiv.visibility = 'visible'
            } else {
                leftMenuDiv.visibility = 'hidden'
            }
        }
        document.querySelectorAll("input[name='" + RADIO_GROUP_NAME_MODES + "']").forEach((input) => {
            input.addEventListener('change', onRadioButtonChecked);
        });
    }
}

function applyTileOnCell(td, selectedTile) {
    td.style.backgroundImage = `url(../assets/textures/${selectedTile}.png)`;
    td.style.backgroundSize = 'cover';
    if (td.cellData) {
        td.cellData.selectedTile = selectedTile
    }
}
