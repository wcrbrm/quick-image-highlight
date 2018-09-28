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
import scalafx.event.ActionEvent
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

object TopPanel {
  def onPasteFromClipboard:Unit = {
    // this.content.text = "Loaded"
  }

  def get = new BorderPane() {
    style = "-fx-background-color: #eee"
    padding = Insets(10, 10, 10, 10)
    left = new Button("Paste From Clipboard") {
      onAction = handle {
        onPasteFromClipboard
      }
    }
    right = new Text {
      text = "Please Load the Image"
      style = "-fx-font: normal 14px sans-serif"
      fill = Color.rgb(0, 0, 0)
    }
  }
}

object CurrentImage {

  def getImageView: ImageView = {
    // val url: URL = getClass().getResource("/no_image.svg");
    //val image: Image = ImageIO.read(url);
    //new ImageView(image)
    val clipboard:Clipboard = Toolkit.getDefaultToolkit.getSystemClipboard

   
      //Get data from clipboard and assign it to an image.
      //clipboard.getData() returns an object, so we need to cast it to a BufferdImage.
      val bufferedImage = clipboard.getData(DataFlavor.imageFlavor).asInstanceOf[BufferedImage]
      // file that we'll save to disk.
      val file: File = new File("d:\\src\\_a_dev\\output.jpg");
      //class to write image to disk.  You specify the image to be saved, its type,
      // and then the file in which to write the image data.
      ImageIO.write(bufferedImage, "jpg", file);
      
      val scalafxImage = SwingFXUtils.toFXImage(bufferedImage, null);
      new ImageView { image = scalafxImage }
  }

  def get = new HBox {
    padding = Insets(50, 80, 50, 80)
    alignment = Pos.Center
    children = Seq( getImageView )
  }

}

object QuickImageHighlight extends JFXApp {

  stage = new PrimaryStage {
    //    initStyle(StageStyle.Unified)
    title = "Please Select Image"
    scene = new Scene {
      fill = Color.rgb(38, 38, 38)
      root = new BorderPane() {
        center =  CurrentImage.get // ContentNoImage.get
        top = TopPanel.get
      }
    }
  }
}
