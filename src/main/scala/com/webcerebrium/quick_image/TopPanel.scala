package com.webcerebrium.quick_image

import scalafx.geometry.{ Insets, Pos }
import scalafx.scene.paint.Color._
import scalafx.scene.paint._
import scalafx.scene.text.Text
import scalafx.scene.image.{Image, ImageView}
import scalafx.scene.layout.{ FlowPane, BorderPane, HBox, VBox }
import scalafx.event.{ ActionEvent, EventHandler }
import scalafx.scene.control._
import scalafx.Includes._

import java.awt.{Toolkit}
import java.awt.datatransfer.{ Clipboard, DataFlavor, UnsupportedFlavorException }

import java.io.File
import javafx.scene.{control => jfxsc, text => jfxst}
import javafx.scene.control.{ToggleButton => JfxToggleBtn}
import javafx.{scene => jfxs}

trait TopPanelTrait {
  def get:BorderPane
  def getButtonByName(name: String): Option[Button]
}


case class TopPanelNoImage(onUpdate: () => Unit) extends TopPanelTrait with ImageChoser {

  val btnPaste = new Button("From Clipboard") { 
    graphic = new ImageView {image = new Image(this, "/paste.png")}
    onAction = handle { 
        CurrentImage.fromClipboard 
        onUpdate()
    }
  }

  val btnOpenDialog = new Button("From Local File") {
    graphic = new ImageView {image = new Image(this, "/open-folder.png")}
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

  def getButtonByName(name: String): Option[Button] = {
    if (name == "PASTE") {
      Some(btnPaste)
    } else if (name == "OPEN") {
      Some(btnOpenDialog)
    } else {
      None
    }
  }
  
  override def get = new BorderPane {
    style = "-fx-background-color: #eee"
    padding = Insets(10, 10, 10, 10)
    center = new HBox {
        children = List( btnOpenDialog, btnPaste )
    }
  }
}

case class TopPanelWithImage(onUpdate: () => Unit, onUpdateMode: (String) => Unit) extends TopPanelTrait {

  var drawingMode = "CROP"
  val radioToggleGroup = new ToggleGroup {
    selectedToggle.onChange(
      (_, oldValue, newValue) => {
        println( "selected: " + newValue.asInstanceOf[JfxToggleBtn].getText )
        onUpdateMode(newValue.asInstanceOf[JfxToggleBtn].getText)
      }
    )
  }
  
  val btnReset = new Button { 
    graphic = new ImageView {image = new Image(this, "/power.png")}
    onAction = handle { 
        CurrentImage.reset 
        onUpdate()
    }
  }
  val btnSave = new Button("Save") { 
    graphic = new ImageView {image = new Image(this, "/save.png")}
    onAction = handle { 
      println("Consider All Saved") 
    }
  }
  val btnCopy = new Button("Copy") { 
    graphic = new ImageView {image = new Image(this, "/copy.png")}
    onAction = handle { 
      CurrentImage.saveToClipboard
      println("Consider Copied to clipboard") 
    }
  }
  val btnShare = new Button("Share") { 
    graphic = new ImageView {image = new Image(this, "/share.png")}
    onAction = handle { println("Consider Exported") }
  }

  val btnCrop = new ToggleButton ("CROP") {
    toggleGroup = radioToggleGroup
    selected = true
  }
  val btnLine = new ToggleButton ("LINE") {
    toggleGroup = radioToggleGroup
  }
  val btnArrow = new ToggleButton ("ARROW") {
    toggleGroup = radioToggleGroup
  }
  val btnBox = new ToggleButton ("BOX") {
    toggleGroup = radioToggleGroup
  }

  def getModeButtons = List( btnCrop, btnLine, btnArrow, btnBox )
  def getModeButton(mode: Int): Option[ToggleButton]= {
    val btnBar = getModeButtons
    if (btnBar.size >= mode) Some(btnBar(mode)) else None
  }
  
  def getButtonByName(name: String): Option[Button] = {
    if (name == "SAVE") {
      Some(btnSave)
    } else if (name == "COPY") {
      Some(btnCopy)
    } else if (name == "SHARE") {
      Some(btnShare)
    } else if (name == "RESET") {
      Some(btnReset)
    } else {
      None
    }
  }
  
  override def get = new BorderPane {
    style = "-fx-background-color: #eee"
    padding = Insets(10, 10, 10, 10)
    left = new HBox(0) {
      children = List( btnCrop, btnLine, btnArrow, btnBox )
    }
    right = new HBox(0) {
      children = List(btnCopy, btnSave, btnShare, btnReset)
    }
  }
}

class TopPanel(onUpdate: () => Unit, onUpdateMode: (String) => Unit) {

  val noImage = TopPanelNoImage(onUpdate)
  val withImage = TopPanelWithImage(onUpdate, onUpdateMode)

  def updateMode: Unit = {
    val flag:Boolean = CurrentImage.isPresent
    println("topPanel.updating mode " + flag.toString)
  }

  def get:BorderPane = {
    if (CurrentImage.isPresent) {
      withImage.get 
    } else {
      noImage.get
    }
  }
  def getModeButtons = withImage.getModeButtons
  def getButton(name: String): Option[Button] = {
  if (CurrentImage.isPresent) {
      withImage.getButtonByName(name)
    } else {
      noImage.getButtonByName(name)
    }
  }

  def getModeButton(mode: Int): Option[ToggleButton] = {
    if (CurrentImage.isPresent) {
      withImage.getModeButton(mode)
    } else {
      None
    }
  }
}