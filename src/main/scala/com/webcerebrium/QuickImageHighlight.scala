package com.webcerebrium

import scalafx.application.JFXApp
import scalafx.application.JFXApp.PrimaryStage
import scalafx.geometry.{ Insets, Pos }
import scalafx.scene.Scene
import scalafx.scene.effect.DropShadow
import scalafx.scene.layout.{ BorderPane, HBox, VBox }
import scalafx.scene.paint.Color._
import scalafx.scene.paint._
import scalafx.scene.text.Text
import scalafx.scene.image.{ Image, ImageView }
import scalafx.scene.input.MouseEvent
import scalafx.scene.control._
import scalafx.scene.control.ButtonBar.ButtonData
import scalafx.Includes._
import scalafx.event.{ ActionEvent, EventHandler }
import scalafx.embed.swing.SwingFXUtils 

import java.awt.Toolkit
import java.awt.datatransfer.{ Clipboard, DataFlavor, UnsupportedFlavorException }
import java.awt.image.BufferedImage

import java.io.{File, IOException }
import java.net.URL
import javax.imageio.ImageIO

object ContentNoImage {
  def getText = new Text {
    text = "Paste Image (Ctrl+V)"
    style = "-fx-font: normal bold 64pt sans-serif"
    fill = new LinearGradient(endX = 0,stops = Stops(Cyan, DarkBlue))
  }
  def getImageView = new ImageView { 
    image = new Image(this, "/no_image.png")
  }
  def get = new HBox {
    padding = Insets(50, 80, 50, 80)
    alignment = Pos.Center
    children = Seq( getImageView )
  }
}

class TopPanel {
// (onPasteFromClipboard: ActionEvent ) {
  def onPasteFromClipboard:Unit = {
    // this.content.text = "Loaded"
  }

  def get = new BorderPane {
    style = "-fx-background-color: #eee"
    padding = Insets(10, 10, 10, 10)
    left = new Button("Paste From Clipboard") {
      onAction = handle { onPasteFromClipboard }
    }
    right = new Text {
      text = "Please Load the Image"
      style = "-fx-font: normal 14px sans-serif"
      fill = Color.rgb(0, 0, 0)
    }
  }
}

object CurrentImage {

  var bufferedImage: Option[BufferedImage] = None

  def fromClipboard: Unit = {
    try {
      val clipboard:Clipboard = Toolkit.getDefaultToolkit.getSystemClipboard
      bufferedImage = Some(clipboard.getData(DataFlavor.imageFlavor).asInstanceOf[BufferedImage])
    } catch {
      case _: Throwable => 
    }
  }
 
  def save(f: File) = {
    bufferedImage.map(bim => ImageIO.write(bim, "jpg", f))
  }

  def getImageView: ImageView = {
    if (bufferedImage.isDefined) {
      new ImageView { image = SwingFXUtils.toFXImage(bufferedImage.get, null) }
    } else {
      ContentNoImage.getImageView
    }
  }

  def get = new BorderPane {
    padding = Insets(10, 10, 10, 10)  
    // alignment = Pos.Center
    style = "-fx-background-color: #822"
    center = new ScrollPane {
      content = getImageView
    }
  }

  fromClipboard
}

object QuickImageHighlight extends JFXApp {

  // def loadFromClipboard(event: EventAction):Unit = {}
  val topPanel = new TopPanel()
  
  stage = new PrimaryStage {
    title = "Please Select Image"
    scene = new Scene {
      root = new BorderPane {
        style = "-fx-background-color: #333"
        center = CurrentImage.get
        top = topPanel.get
      }
    }
  }
}
