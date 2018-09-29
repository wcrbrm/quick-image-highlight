package com.webcerebrium.quick_image

import scalafx.geometry.{ Insets, Pos }
import scalafx.scene.paint.Color._
import scalafx.scene.paint._
import scalafx.scene.text.Text
import scalafx.scene.layout.{ FlowPane, BorderPane, HBox, VBox }
import scalafx.event.{ ActionEvent, EventHandler }
import scalafx.scene.control._
import scalafx.Includes._

trait TopPanelTrait {
  def get:BorderPane
}

case class TopPanelNoImage(onUpdate: () => Unit) extends TopPanelTrait {

  val btnPaste = new Button("Paste From Clipboard") { 
    onAction = handle { 
        CurrentImage.fromClipboard 
        onUpdate()
    }
  }

  override def get = new BorderPane {
    style = "-fx-background-color: #eee"
    padding = Insets(10, 10, 10, 10)
    left = new HBox {
        children = btnPaste
    }
    right = new Text {
        text = "Please Load the Image"
        style = "-fx-font: normal 14px sans-serif"
        fill = Color.rgb(0, 0, 0)
    }
  }
}

case class TopPanelWithImage(onUpdate: () => Unit) extends TopPanelTrait {
  val btnReset = new Button("Reset") { 
    onAction = handle { 
        CurrentImage.reset 
        onUpdate()
    }
  }
  override def get = new BorderPane {
    style = "-fx-background-color: #eee"
    padding = Insets(10, 10, 10, 10)
    left = new HBox {
        children = btnReset
    }
  }
}

class TopPanel(onUpdate: () => Unit) {

  def updateMode: Unit = {
    val flag:Boolean = CurrentImage.isPresent
    println("topPanel.updating mode " + flag.toString)
  }

  def get:BorderPane = {
    if (CurrentImage.isPresent) {
      TopPanelWithImage(onUpdate).get 
    } else {
      TopPanelNoImage(onUpdate).get
    }
  }
}