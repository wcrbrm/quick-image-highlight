package com.webcerebrium.quick_image

import scalafx.geometry.{ Insets, Pos }
import scalafx.scene.paint.Color._
import scalafx.scene.paint._
import scalafx.scene.text.Text
import scalafx.scene.layout.{ FlowPane, BorderPane, HBox, VBox }
import scalafx.event.{ ActionEvent, EventHandler }
import scalafx.scene.control._
import scalafx.Includes._

class TopPanel(onUpdate: () => Unit) {

  def updateMode: Unit = {
    val flag:Boolean = CurrentImage.isPresent
    println("topPanel.updating mode " + flag.toString)
  }

  val btnPaste = new Button("Paste From Clipboard") { 
    onAction = handle { CurrentImage.fromClipboard }
    visible = false
  }
  val btnReset = new Button("Reset") { 
    onAction = handle { CurrentImage.reset }
    visible = true
  }
  

  def get = new BorderPane {
    style = "-fx-background-color: #eee"
    padding = Insets(10, 10, 10, 10)
    left = new HBox {
      children = List(btnPaste, btnReset)
    }
    right = new Text {
      text = "Please Load the Image"
      style = "-fx-font: normal 14px sans-serif"
      fill = Color.rgb(0, 0, 0)
      visible = false
    }
  }
}