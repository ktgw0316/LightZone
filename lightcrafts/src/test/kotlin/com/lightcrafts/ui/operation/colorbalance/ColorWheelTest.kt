package com.lightcrafts.ui.operation.colorbalance

import com.lightcrafts.ui.swing.ColorSwatch
import com.lightcrafts.ui.toolkit.DropperButton
import java.awt.Color
import java.awt.Component
import javax.swing.BorderFactory
import javax.swing.Box
import javax.swing.JFrame
import javax.swing.SwingUtilities

inline fun jFrame(title: String, action: JFrame.() -> Unit) = JFrame(title).apply(action)
inline fun horizontalBox(action: Box.() -> Unit): Box = Box.createHorizontalBox().apply(action)
fun horizontalStrut(width: Int): Component = Box.createHorizontalStrut(width)

private fun createAndShowGUI() {
    val wheel = ColorWheel()
    val colorSwatch = ColorSwatch(Color.gray)
    val colorText = ColorText(Color.gray)

    val listener = object : ColorWheelMouseListener(wheel) {
        override fun colorPicked(color: Color, isChanging: Boolean) {
            colorSwatch.setColor(color)
            colorText.setColor(color)
        }
    }

    jFrame("ColorWheelTest") {
        contentPane = horizontalBox {
            add(wheel.apply {
                addMouseListener(listener)
                addMouseMotionListener(listener)
            })
            add(horizontalStrut(8))
            add(colorSwatch)
            add(horizontalStrut(8))
            add(colorText)
            add(horizontalStrut(8))
            add(DropperButton())
            border = BorderFactory.createEmptyBorder(3, 3, 3, 3)
        }
        defaultCloseOperation = JFrame.EXIT_ON_CLOSE
        pack()
        setLocationRelativeTo(null)
        isVisible = true
    }
}

fun main() {
    SwingUtilities.invokeLater {
        createAndShowGUI()
    }
}
