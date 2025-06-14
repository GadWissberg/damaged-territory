package com.gadarts.dte.lwjgl3

import com.badlogic.gdx.ai.msg.Telegram
import com.badlogic.gdx.ai.msg.Telegraph
import com.gadarts.dte.EditorEvents
import java.io.File
import javax.swing.JFileChooser
import javax.swing.SwingUtilities

class DesktopListener : Telegraph {
    override fun handleMessage(telegram: Telegram?): Boolean {
        if (telegram == null) return false

        return when (telegram.message) {
            EditorEvents.SAVE_MAP.ordinal -> {
                val json = telegram.extraInfo as String
                SwingUtilities.invokeLater {
                    val chooser = JFileChooser()
                    chooser.dialogTitle = "Save Map As"
                    chooser.selectedFile = File("map.json")

                    val result = chooser.showSaveDialog(null)

                    if (result == JFileChooser.APPROVE_OPTION) {
                        val file = chooser.selectedFile

                        file.writeText(json)

                        println("Saved to: ${file.absolutePath}")
                    }
                }
                true
            }

            else -> false
        }
    }

}
