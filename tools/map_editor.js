class CellData {
  constructor() {
    (this.object = null), (this.value = 0);
  }
}
document.querySelectorAll("#itemList li").forEach((item) => {
  item.addEventListener("click", () => {
    item.classList.toggle("selected");
  });
});
let layerCount = 1;
function attachClickHandlers() {
  document.querySelectorAll("#itemList li").forEach((item) => {
    item.onclick = () => item.classList.toggle("selected");
  });
}
const layers = []; // Array to store overlay tables

document.getElementById("addLayerButton").addEventListener("click", () => {
  const userInput = prompt("Layer name:");
  if (userInput && userInput.trim() !== "") {
    const newItem = document.createElement("li");
    newItem.textContent = userInput;
    newItem.onclick = () => newItem.classList.toggle("selected");
    document.getElementById("layerList").appendChild(newItem);
    const overlay = document.createElement("table");
    mapContainer.appendChild(overlay);

    Object.assign(overlay.style, {
      width: MAP_SIZE * TILE_SIZE + "px",
      height: MAP_SIZE * TILE_SIZE + "px",
      position: "absolute",
      top: "0",
      left: "0",
      zIndex: (layers.length + 2).toString(), // Stack above existing layers
      pointerEvents: "none", // Optional: allow clicks to pass through
      borderCollapse: "collapse",
    });

    // Fill the new layer with transparent/colored cells
    for (let y = 0; y < MAP_SIZE; y++) {
      const row = overlay.insertRow();
      for (let x = 0; x < MAP_SIZE; x++) {
        const cell = row.insertCell();
        Object.assign(cell.style, {
          width: TILE_SIZE + "px",
          height: TILE_SIZE + "px",
          backgroundColor: "rgba(0, 255, 0, 0.1)", // green overlay
          border: "1px solid transparent",
        });
      }
    }

    // Save the overlay in the layers array
    layers.push(overlay);
  }
});
document.querySelectorAll("#itemList li").forEach((item) => {
  item.addEventListener("click", () => {
    item.classList.toggle("selected");
  });
});
attachClickHandlers();
const ID_MAP = "map";
const Modes = Object.freeze({
  TILES: Symbol("tiles"),
  OBJECTS: Symbol("objects"),
});
const elementsDefinitions = Object.freeze([
  { name: "PLAYER", type: "CHARACTER" },
  { name: "PALM_TREE", type: "AMB" },
  { name: "GUARD_HOUSE", type: "AMB" },
  { name: "WATCH_TOWER", type: "AMB" },
  { name: "BUILDING_FLAG", type: "AMB" },
  { name: "BASE_BROWN", type: "AMB" },
  { name: "BASE_GREEN", type: "AMB" },
  { name: "ROCK_BIG", type: "AMB" },
  { name: "ROCK_MED", type: "AMB" },
  { name: "ROCK_SMALL", type: "AMB" },
  { name: "BUILDING_0", type: "AMB" },
  { name: "ANTENNA", type: "AMB" },
  { name: "STREET_LIGHT", type: "AMB" },
  { name: "FENCE", type: "AMB" },
  { name: "RUINS", type: "AMB" },
  { name: "HANGAR", type: "AMB" },
  { name: "SIGN", type: "AMB" },
  { name: "SIGN_BIG", type: "AMB" },
  { name: "TURRET_CANNON", type: "CHARACTER" },
]);
const Directions = Object.freeze({ east: 0, north: 90, west: 180, south: 270 });
const MAP_SIZES = Object.freeze({ small: 48, medium: 96, large: 192 });
const TILE_BRUSHES = Object.freeze(["DYNAMIC", "MANUAL"]);
const body = document.body;
const table = document
  .getElementById(ID_MAP)
  .appendChild(document.createElement("table"));
const RADIO_GROUP_NAME_MODES = "modes";
const OPTION_MODE_OBJECTS = "option_mode_objects";
const DIV_ID_LEFT_MENU = "left_menu_div";
const CLASS_NAME_GAME_OBJECT_SELECTION = "game_object_selection";
const CLASS_NAME_GAME_OBJECT_RADIO = "game_object_radio";
const RADIO_GROUP_NAME_GAME_OBJECT_SELECTIONS = "game_object_selections";
const DIV_ID_BUTTON_SAVE = "button_save";
const DIV_ID_BUTTON_LOAD = "button_load";
const OUTPUT_FILE_NAME = "map.json";
const SELECT_ID_DROPDOWN_MAP_SIZES = "dropdown_map_sizes";
const SELECT_ID_DROPDOWN_TILE_BRUSHES = "dropdown_tile_brushes";
const SELECT_ID_DROPDOWN_TILES = "dropdown_tiles";
const DIV_ID_DIRECTION = "direction";
const DIV_ID_CELL_CONTENTS = "cell_contents";
const CLASS_NAME_CELL_CONTENTS = "cellContents";
const BIT_GROUND = 2;
const BIT_SHALLOW_WATER = 1;
const BIT_DEEP_WATER = 0;
const TILES_CHARS = Array.from({ length: 80 }, (_, i) =>
  String.fromCharCode(i + 48)
).join("");

const maskIndices = [
  0b11111111, 0b01111111, 0b11011111, 0b11111011, 0b11111110, 0b00011111,
  0b01101011, 0b11010110, 0b11111000, 0b00001011, 0b00010110, 0b01101000,
  0b11010000,
];

const tilesMaskMapping = [];
tilesMaskMapping[0b11010000] = "tile_?_bottom_right";
tilesMaskMapping[0b01101000] = "tile_?_bottom_left";
tilesMaskMapping[0b00010110] = "tile_?_top_right";
tilesMaskMapping[0b00001011] = "tile_?_top_left";
tilesMaskMapping[0b11111000] = "tile_?_bottom";
tilesMaskMapping[0b11010110] = "tile_?_right";
tilesMaskMapping[0b01101011] = "tile_?_left";
tilesMaskMapping[0b00011111] = "tile_?_top";
tilesMaskMapping[0b11111110] = "tile_?_gulf_bottom_right";
tilesMaskMapping[0b11111011] = "tile_?_gulf_bottom_left";
tilesMaskMapping[0b11011111] = "tile_?_gulf_top_right";
tilesMaskMapping[0b01111111] = "tile_?_gulf_top_left";
tilesMaskMapping[0b11111111] = "tile_?";

const tiles = [
  { name: "tile_water", animated: true, bit: BIT_DEEP_WATER },
  { name: "tile_beach_bottom_right", animated: true, bit: BIT_GROUND },
  { name: "tile_beach_gulf_bottom_right", animated: true, bit: BIT_GROUND },
  { name: "tile_beach_bottom", animated: true, bit: BIT_GROUND },
  { name: "tile_beach_bottom_special", animated: true, bit: BIT_GROUND },
  { name: "tile_beach_bottom_left", animated: true, bit: BIT_GROUND },
  { name: "tile_beach_gulf_bottom_left", animated: true, bit: BIT_GROUND },
  { name: "tile_beach_right", animated: true, bit: BIT_GROUND },
  { name: "tile_beach_left", animated: true, bit: BIT_GROUND },
  { name: "tile_beach_top_right", animated: true, bit: BIT_GROUND },
  { name: "tile_beach_gulf_top_right", animated: true, bit: BIT_GROUND },
  { name: "tile_beach_top", animated: true, bit: BIT_GROUND },
  { name: "tile_beach_top_left", animated: true, bit: BIT_GROUND },
  { name: "tile_beach_gulf_top_left", animated: true, bit: BIT_GROUND },
  { name: "tile_beach", animated: false, bit: BIT_GROUND },
  {
    name: "tile_water_shallow_bottom_right",
    animated: true,
    bit: BIT_SHALLOW_WATER,
  },
  {
    name: "tile_water_shallow_gulf_bottom_right",
    animated: true,
    bit: BIT_SHALLOW_WATER,
  },
  { name: "tile_water_shallow_bottom", animated: true, bit: BIT_SHALLOW_WATER },
  {
    name: "tile_water_shallow_bottom_left",
    animated: true,
    bit: BIT_SHALLOW_WATER,
  },
  {
    name: "tile_water_shallow_gulf_bottom_left",
    animated: true,
    bit: BIT_SHALLOW_WATER,
  },
  { name: "tile_water_shallow_right", animated: true, bit: BIT_SHALLOW_WATER },
  { name: "tile_water_shallow_left", animated: true, bit: BIT_SHALLOW_WATER },
  {
    name: "tile_water_shallow_top_right",
    animated: true,
    bit: BIT_SHALLOW_WATER,
  },
  {
    name: "tile_water_shallow_gulf_top_right",
    animated: true,
    bit: BIT_SHALLOW_WATER,
  },
  { name: "tile_water_shallow_top", animated: true, bit: BIT_SHALLOW_WATER },
  {
    name: "tile_water_shallow_top_left",
    animated: true,
    bit: BIT_SHALLOW_WATER,
  },
  {
    name: "tile_water_shallow_gulf_top_left",
    animated: true,
    bit: BIT_SHALLOW_WATER,
  },
  { name: "tile_water_shallow", animated: true, bit: BIT_SHALLOW_WATER },
  { name: "tile_beach_road_horizontal", animate: false, bit: BIT_GROUND },
  { name: "tile_beach_road_horizontal_top", animate: false, bit: BIT_GROUND },
  {
    name: "tile_beach_road_horizontal_bottom",
    animate: false,
    bit: BIT_GROUND,
  },
  {
    name: "tile_beach_road_horizontal_end_right",
    animate: false,
    bit: BIT_GROUND,
  },
  {
    name: "tile_beach_road_horizontal_end_left",
    animate: false,
    bit: BIT_GROUND,
  },
  { name: "tile_beach_road_vertical", animate: false, bit: BIT_GROUND },
  { name: "tile_beach_road_vertical_right", animate: false, bit: BIT_GROUND },
  { name: "tile_beach_road_vertical_left", animate: false, bit: BIT_GROUND },
  { name: "tile_beach_road_vertical_end_up", animate: false, bit: BIT_GROUND },
  {
    name: "tile_beach_road_vertical_end_down",
    animate: false,
    bit: BIT_GROUND,
  },
  { name: "tile_beach_road_bottom_right", animate: false, bit: BIT_GROUND },
  { name: "tile_beach_road_bottom_left", animate: false, bit: BIT_GROUND },
  { name: "tile_beach_road_top_left", animate: false, bit: BIT_GROUND },
  { name: "tile_beach_road_top_right", animate: false, bit: BIT_GROUND },
  { name: "tile_beach_road_cross", animate: false, bit: BIT_GROUND },
  { name: "tile_beach_grass", animate: false, bit: BIT_GROUND },
  { name: "tile_beach_grass_top", animate: false, bit: BIT_GROUND },
  { name: "tile_beach_grass_right", animate: false, bit: BIT_GROUND },
  { name: "tile_beach_grass_bottom", animate: false, bit: BIT_GROUND },
  { name: "tile_beach_grass_left", animate: false, bit: BIT_GROUND },
  { name: "tile_beach_grass_top_right", animate: false, bit: BIT_GROUND },
  { name: "tile_beach_grass_top_left", animate: false, bit: BIT_GROUND },
  { name: "tile_beach_grass_bottom_right", animate: false, bit: BIT_GROUND },
  { name: "tile_beach_grass_bottom_left", animate: false, bit: BIT_GROUND },
  { name: "tile_beach_grass_gulf_top_right", animate: false, bit: BIT_GROUND },
  { name: "tile_beach_grass_gulf_top_left", animate: false, bit: BIT_GROUND },
  {
    name: "tile_beach_grass_gulf_bottom_right",
    animate: false,
    bit: BIT_GROUND,
  },
  {
    name: "tile_beach_grass_gulf_bottom_left",
    animate: false,
    bit: BIT_GROUND,
  },
  { name: "tile_beach_dark", animate: false, bit: BIT_GROUND },
  { name: "tile_beach_dark_top", animate: false, bit: BIT_GROUND },
  { name: "tile_beach_dark_right", animate: false, bit: BIT_GROUND },
  { name: "tile_beach_dark_bottom", animate: false, bit: BIT_GROUND },
  { name: "tile_beach_dark_left", animate: false, bit: BIT_GROUND },
  { name: "tile_beach_dark_top_right", animate: false, bit: BIT_GROUND },
  { name: "tile_beach_dark_top_left", animate: false, bit: BIT_GROUND },
  { name: "tile_beach_dark_bottom_right", animate: false, bit: BIT_GROUND },
  { name: "tile_beach_dark_bottom_left", animate: false, bit: BIT_GROUND },
  { name: "tile_beach_dark_gulf_top_right", animate: false, bit: BIT_GROUND },
  { name: "tile_beach_dark_gulf_top_left", animate: false, bit: BIT_GROUND },
  {
    name: "tile_beach_dark_gulf_bottom_right",
    animate: false,
    bit: BIT_GROUND,
  },
  { name: "tile_beach_dark_gulf_bottom_left", animate: false, bit: BIT_GROUND },
];
class MapEditor {
  constructor() {
    this.resetMap();
    this.initializeModesRadioButtons();
    this.inflateElementsLeftMenu();
    const mapSizesDropDown = fillDropdownOptions(
      SELECT_ID_DROPDOWN_MAP_SIZES,
      Object.keys(MAP_SIZES)
    );
    document
      .getElementById(SELECT_ID_DROPDOWN_MAP_SIZES)
      .addEventListener("change", function () {
        self.resetMap(options[mapSizesDropDown.value]);
      });
    const tileBrushesDropDown = fillDropdownOptions(
      SELECT_ID_DROPDOWN_TILE_BRUSHES,
      Object.values(TILE_BRUSHES)
    );
    document
      .getElementById(SELECT_ID_DROPDOWN_TILE_BRUSHES)
      .addEventListener("change", function () {
        const manualSelector = document.getElementById("manualSelector");
        if (tileBrushesDropDown.value === "MANUAL") {
          manualSelector.style.visibility = "visible";
        } else {
          manualSelector.style.visibility = "hidden";
        }
      });
    fillDropdownOptions(
      SELECT_ID_DROPDOWN_TILES,
      tiles.map((tileObject) => tileObject.name)
    );
    table.style.width = this.map_size * 64 + "px";
    table.style.height = this.map_size * 64 + "px";
    var self = this;
    defineSaveProcess();
    defineLoadProcess();

    function fillDropdownOptions(id, options) {
      var dropDown = document.getElementById(id);
      for (const element of options) {
        var option = document.createElement("option");
        option.value = element;
        option.text = element;
        dropDown.appendChild(option);
      }
      return dropDown;
    }

    function defineLoadProcess() {
      document
        .getElementById(DIV_ID_BUTTON_LOAD)
        .addEventListener("click", () => {
          var input = document.createElement("input");
          input.type = "file";
          input.addEventListener(
            "change",
            (e) => {
              var file = e.target.files[0];
              if (!file) {
                return;
              }
              var reader = new FileReader();
              reader.onload = (e) => {
                var contents = e.target.result;
                var inputMapObject = JSON.parse(contents);
                self.resetMap(inputMapObject.size);
                inflateTiles();
                for (var i = 0; i < inputMapObject.elements.length; i++) {
                  var element = inputMapObject.elements[i];
                  self.placeElementObject(
                    table.rows[element.row].cells[element.col],
                    element.definition,
                    element.direction
                  );
                }

                function inflateTiles() {
                  for (var i = 0; i < inputMapObject.tiles.length; i++) {
                    var cell =
                      table.rows[Math.floor(i / self.map_size)].cells[
                        i % self.map_size
                      ];
                    var placedTile =
                      tiles[
                        TILES_CHARS.indexOf(inputMapObject.tiles.charAt(i))
                      ];
                    cell.cellData = new CellData();
                    applyTileOnCell(cell, placedTile, placedTile.bit);
                    cell.style.backgroundColor = placedTile.tile;
                  }
                }
              };
              reader.readAsText(file);
            },
            false
          );
          input.click();
        });
    }

    function defineSaveProcess() {
      document
        .getElementById(DIV_ID_BUTTON_SAVE)
        .addEventListener("click", (e) => {
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
              elementObject.type = elementsDefinitions.find(
                (obj) => obj.name === object.definition
              ).type;
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
                  const tileName =
                    typeof cellData.selectedTile !== "string"
                      ? cellData.selectedTile.name
                      : cellData.selectedTile;
                  const result = tiles.find(
                    (tileObject) => tileObject.name === tileName
                  );
                  if (result) {
                    currentTile = TILES_CHARS[tiles.indexOf(result)];
                  }
                }
                tilesString += currentTile;
              }
            }
            return tilesString;
          }

          function saveJsonToFile(json) {
            var bb = new Blob([json], { type: "text/json" });
            var a = document.createElement("a");
            a.download = OUTPUT_FILE_NAME;
            a.href = window.URL.createObjectURL(bb);
            a.click();
          }
        });
    }
  }

  inflateElementsLeftMenu() {
    var leftMenu = document.getElementById(DIV_ID_LEFT_MENU);
    elementsDefinitions.forEach((element) => {
      var div = document.createElement("div");
      var radioButton = addRadioButtonForElement(div);
      div.className = CLASS_NAME_GAME_OBJECT_SELECTION;
      var label = document.createElement("label");
      label.for = radioButton.id;
      label.appendChild(document.createTextNode(element.name));
      div.appendChild(label);
      leftMenu.appendChild(div);

      function addRadioButtonForElement(div) {
        var radioButton = document.createElement("input");
        radioButton.type = "radio";
        radioButton.className = CLASS_NAME_GAME_OBJECT_RADIO;
        radioButton.name = RADIO_GROUP_NAME_GAME_OBJECT_SELECTIONS;
        radioButton.value = element.name;
        radioButton.valueObject = element;
        radioButton.id = "element_selection_" + element;
        div.appendChild(radioButton);
        return radioButton;
      }
    });
  }

  findChildTextNode(cellContents) {
    if (cellContents == null) return;
    var textNode = null;
    for (var i = 0; i < cellContents.childNodes.length; i++) {
      var curNode = cellContents.childNodes[i];
      if (curNode.nodeName === "#text") {
        textNode = curNode;
        break;
      }
    }
    return textNode;
  }

  placeElementInCell(cell, cellData, input, row, col) {
    var selectedMode =
      Modes[
        document.querySelector(
          'input[name="' + RADIO_GROUP_NAME_MODES + '"]:checked'
        ).value
      ];
    var self = this;
    var leftClick = input == "click";
    if (selectedMode == Modes.TILES) {
      const dropDown = document.getElementById(SELECT_ID_DROPDOWN_TILE_BRUSHES);
      if (dropDown.value === "MANUAL") {
        const tilesDropDown = document.getElementById(SELECT_ID_DROPDOWN_TILES);
        this.applyTileChangeInCell(
          cellData,
          cell,
          row,
          col,
          tiles.find((tileObject) => tileObject.name === tilesDropDown.value),
          false
        );
      } else {
        this.applyTileChangeInCell(
          cellData,
          cell,
          row,
          col,
          "tile_beach",
          true
        );
      }
    } else if (selectedMode == Modes.OBJECTS) {
      applyElementChangeInCell();
    }

    function applyElementChangeInCell() {
      if (leftClick) {
        var selection = document.querySelector(
          'input[name="' +
            RADIO_GROUP_NAME_GAME_OBJECT_SELECTIONS +
            '"]:checked'
        ).valueObject.name;
        self.placeElementObject(cell, selection);
      } else {
        removeElementObject(editor, cell);
      }
    }

    function removeElementObject(editor, cell) {
      var textNode = editor.getOrAddChildTextNode(cell);
      cellData.object = null;
      textNode.nodeValue = null;
      var directionDiv = document.getElementById(
        DIV_ID_DIRECTION +
          "_" +
          cell.closest("tr").rowIndex +
          "_" +
          cell.cellIndex
      );
      if (directionDiv != null) {
        directionDiv.removeChild(directionDiv.getElementsByTagName("img")[0]);
      }
    }
  }

  applyTileChangeInCell(cellData, cell, row, col, tileName, surround) {
    cellData.value = BIT_GROUND;
    applyTileOnCell(cell, tileName);
    if (surround) {
      this.applyGroundTiles(row, col);
      this.applyShallowWaterTiles(row, col);
    }
  }

  applyShallowWaterTiles(row, col) {
    for (let adjRow = row - 2; adjRow < row + 3; adjRow++) {
      for (let adjCol = col - 2; adjCol < col + 3; adjCol++) {
        if (
          adjRow >= 0 &&
          adjRow < this.map_size &&
          adjCol >= 0 &&
          adjCol < this.map_size &&
          (adjRow != row || adjCol != col)
        ) {
          var adjCell = table.rows[adjRow].cells[adjCol];
          this.initializeCellData(adjCell);
          if (adjCell.cellData.value < BIT_SHALLOW_WATER) {
            adjCell.cellData.value = BIT_SHALLOW_WATER;
          }
        }
      }
    }

    for (let adjRow = row - 2; adjRow < row + 3; adjRow++) {
      for (let adjCol = col - 2; adjCol < col + 3; adjCol++) {
        if (
          adjRow >= 0 &&
          adjRow < this.map_size &&
          adjCol >= 0 &&
          adjCol < this.map_size &&
          (adjRow != row || adjCol != col)
        ) {
          var adjCell = table.rows[adjRow].cells[adjCol];
          if (!adjCell || adjCell.cellData.value <= BIT_SHALLOW_WATER) {
            const mask = this.calculateMask(adjRow, adjCol, BIT_SHALLOW_WATER);
            var tile = tilesMaskMapping[mask];
            if (!tile) {
              for (let i = 0; i < maskIndices.length; i++) {
                let element = maskIndices[i];
                if ((element & mask) == element) {
                  tile = tilesMaskMapping[element];
                  break;
                }
              }
            }

            if (tile) {
              tile = tile.replace("?", "water_shallow");
              applyTileOnCell(
                adjCell,
                tiles.find((tileObject) => tileObject.name === tile)
              );
            }
          }
        }
      }
    }
  }

  applyGroundTiles(row, col) {
    for (let adjRow = row - 1; adjRow < row + 2; adjRow++) {
      for (let adjCol = col - 1; adjCol < col + 2; adjCol++) {
        if (
          adjRow >= 0 &&
          adjRow < this.map_size &&
          adjCol >= 0 &&
          adjCol < this.map_size &&
          (adjRow != row || adjCol != col)
        ) {
          var adjCell = table.rows[adjRow].cells[adjCol];
          this.initializeCellData(adjCell);
          adjCell.cellData.value = BIT_GROUND;
        }
      }
    }
    for (let adjRow = row - 1; adjRow < row + 2; adjRow++) {
      for (let adjCol = col - 1; adjCol < col + 2; adjCol++) {
        if (
          adjRow >= 0 &&
          adjRow < this.map_size &&
          adjCol >= 0 &&
          adjCol < this.map_size &&
          (adjRow != row || adjCol != col)
        ) {
          var adjCell = table.rows[adjRow].cells[adjCol];
          const mask = this.calculateMask(adjRow, adjCol, BIT_GROUND);
          var tile = tilesMaskMapping[mask];
          if (!tile) {
            for (let i = 0; i < maskIndices.length; i++) {
              let element = maskIndices[i];
              if ((element & mask) == element) {
                tile = tilesMaskMapping[element];
                break;
              }
            }
          }

          if (tile) {
            tile = tile.replace("?", "beach");
            applyTileOnCell(
              adjCell,
              tiles.find((tileObject) => tileObject.name === tile)
            );
          }
        }
      }
    }
  }

  calculateMask(row, col, highValue) {
    var mask = 0;
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
      mask |= fetchValueFromCell(row + 1, col + 1) & 0b00000001;
    }
    return mask;

    function fetchValueFromCell(row, col) {
      const cellData = table.rows[row].cells[col].cellData;
      if (cellData) {
        return cellData.value >= highValue ? 1 : 0;
      } else {
        return 0;
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
      var directionDiv = document.getElementById(
        DIV_ID_DIRECTION +
          "_" +
          cell.closest("tr").rowIndex +
          "_" +
          cell.cellIndex
      );
      if (directionDiv == null) {
        directionDiv = createArrowImage();
      }
      directionDiv.getElementsByTagName("img")[0].style.transform =
        "translate(-50%, -50%) rotate(" + -1 * direction + "deg) ";
      return directionDiv;
    }

    function createArrowImage() {
      directionDiv = document.createElement("div");
      directionDiv.id =
        DIV_ID_DIRECTION +
        "_" +
        cell.closest("tr").rowIndex +
        "_" +
        cell.cellIndex;
      var imageElement = document.createElement("img");
      imageElement.src = "arrow.png";
      directionDiv.appendChild(imageElement);
      return document
        .getElementById(
          DIV_ID_CELL_CONTENTS +
            "_" +
            cell.closest("tr").rowIndex +
            "_" +
            cell.cellIndex
        )
        .appendChild(directionDiv);
    }
  }

  onCellLeftClicked(row, col) {
    var cell = table.rows[row].cells[col];
    this.initializeCellData(cell);
    var cellData = cell.cellData;
    this.placeElementInCell(cell, cellData, "click", row, col);
  }

  onCellRightClicked(row, col) {
    var cell = table.rows[row].cells[col];
    this.initializeCellData(cell);
    var cellData = cell.cellData;
    var selectedMode =
      Modes[
        document.querySelector(
          'input[name="' + RADIO_GROUP_NAME_MODES + '"]:checked'
        ).value
      ];
    this.placeElementInCell(cell, cellData, selectedMode, "contextmenu");
  }

  getOrAddChildTextNode(cell) {
    var cellContentsId =
      DIV_ID_CELL_CONTENTS +
      "_" +
      cell.closest("tr").rowIndex +
      "_" +
      cell.cellIndex;
    var textNode = this.findChildTextNode(
      document.getElementById(cellContentsId)
    );
    if (textNode == null) {
      createCellContents();
    }
    return textNode;

    function createCellContents() {
      var cellContents = document.createElement("div");
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
    document.getElementById(SELECT_ID_DROPDOWN_MAP_SIZES).value =
      findMapSizeDefinitionByValue(this.map_size);
    for (let i = 0; i < this.map_size; i++) {
      const tr = table.insertRow();
      for (let j = 0; j < this.map_size; j++) {
        const td = tr.insertCell();
        applyTileOnCell(td, { name: "tile_water", animated: true });
        td.classList.add("cell");
        td.addEventListener("click", (e) => {
          this.onCellLeftClicked(i, j);
        });
        td.addEventListener(
          "contextmenu",
          (e) => {
            e.preventDefault();
            this.onCellRightClicked(i, j);
            return false;
          },
          false
        );
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
        leftMenuDiv.visibility = "visible";
      } else {
        leftMenuDiv.visibility = "hidden";
      }
    }
    document
      .querySelectorAll("input[name='" + RADIO_GROUP_NAME_MODES + "']")
      .forEach((input) => {
        input.addEventListener("change", onRadioButtonChecked);
      });
  }
}

function applyTileOnCell(td, selectedTile, bit = -1) {
  if (!selectedTile) return;

  let finalName = selectedTile;
  if (typeof selectedTile !== "string") {
    finalName = selectedTile.name + (selectedTile.animated ? "_0" : "");
  }
  td.style.backgroundImage = `url(../assets/textures/ground/beach/${finalName}.png)`;
  td.style.backgroundSize = "cover";
  if (td.cellData) {
    td.cellData.selectedTile = selectedTile;
    if (bit >= 0) {
      td.cellData.value = bit;
    }
  }
}
