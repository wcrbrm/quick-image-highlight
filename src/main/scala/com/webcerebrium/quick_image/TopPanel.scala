package com.webcerebrium.quick_image

import scalafx.geometry.{ Insets, Pos }
import scalafx.scene.paint.Color._
import scalafx.scene.paint._
import scalafx.scene.text.Text
import scalafx.scene.layout.{ FlowPane, BorderPane, HBox, VBox }
import scalafx.event.{ ActionEvent, EventHandler }
import scalafx.scene.control._
import scalafx.Includes._

import java.awt.{Toolkit}
import java.awt.datatransfer.{ Clipboard, DataFlavor, UnsupportedFlavorException }

import java.io.File
import javafx.scene.{control => jfxsc, text => jfxst}
import javafx.{scene => jfxs}

trait TopPanelTrait {
  def get:BorderPane
  def getMode:String
}


case class TopPanelNoImage(onUpdate: () => Unit) extends TopPanelTrait with ImageChoser {

  val btnPaste = new Button("From Clipboard") { 
    onAction = handle { 
        CurrentImage.fromClipboard 
        onUpdate()
    }
  }

  val btnOpenDialog = new Button("From Local File") {
    onAction = handle {
      val imageFile: Option[File] = selectImage("Please select local image")
      if (imageFile.isDefined) {
        CurrentImage.fromFile(imageFile.get)
        onUpdate()
      } else {
        println("File not selected")
      }
    }
  }

  override def getMode:String  = ""
  override def get = new BorderPane {
    style = "-fx-background-color: #eee"
    padding = Insets(10, 10, 10, 10)
    center = new HBox {
        children = List( btnOpenDialog, btnPaste )
    }
  }
}

case class TopPanelWithImage(onUpdate: () => Unit, onUpdateMode: (String) => Unit) extends TopPanelTrait {

  var drawingMode = "crop"
  val radioToggleGroup = new ToggleGroup
  
  val btnReset = new Button("Reset") { 
    style = "-fx-background-color: #fff"
    onAction = handle { 
        CurrentImage.reset 
        onUpdate()
    }
  }
  val btnSave = new Button("Save") { 
    style = "-fx-background-color: #faa; -fx-cursor: pointer"
    onAction = handle { println("Consider All Saved") }
  }
  
  override def getMode:String = radioToggleGroup.selectedToggle().asInstanceOf[jfxsc.RadioButton].text()
  override def get = new BorderPane {
    style = "-fx-background-color: #eee"
    padding = Insets(10, 10, 10, 10)
    left = new HBox(8) {
      children = List(
        new RadioButton("CROP") {
            toggleGroup = radioToggleGroup
            selected = true
            onAction = handle { onUpdateMode(getMode) }
        },
        new RadioButton("LINE") {
            toggleGroup = radioToggleGroup
            onAction = handle { onUpdateMode(getMode) }
        },
        new RadioButton("ARROW") {
            toggleGroup = radioToggleGroup
            onAction = handle { onUpdateMode(getMode) }
        },
        new RadioButton("BOX") {
            toggleGroup = radioToggleGroup
            onAction = handle { onUpdateMode(getMode) }
        }
      )
    }
    right = new HBox(20) {
      children = List(btnReset, btnSave)
    }
  }
}

class TopPanel(onUpdate: () => Unit, onUpdateMode: (String) => Unit) {

  def updateMode: Unit = {
    val flag:Boolean = CurrentImage.isPresent
    println("topPanel.updating mode " + flag.toString)
  }

  def get:BorderPane = {
    if (CurrentImage.isPresent) {
      TopPanelWithImage(onUpdate, onUpdateMode).get 
    } else {
      TopPanelNoImage(onUpdate).get
    }
  }
}