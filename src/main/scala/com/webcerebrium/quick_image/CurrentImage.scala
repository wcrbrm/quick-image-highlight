package com.webcerebrium.quick_image

import scala.util.Try
import scalafx.geometry.{ Insets, Pos }
import scalafx.scene.image.{ Image, ImageView }
import scalafx.scene.layout.{ BorderPane, HBox, VBox }
import scalafx.scene.control._
import java.awt.{Toolkit, Graphics2D}
import java.awt.datatransfer.{ Clipboard, DataFlavor, UnsupportedFlavorException }
import java.awt.image.BufferedImage
import scalafx.embed.swing.SwingFXUtils 

import java.io.{File, IOException }
import java.net.URL
import javax.imageio.ImageIO

object CurrentImage {

  var bufferedImage: Option[BufferedImage] = None

  def fromClipboard: Unit = {
    try {
      val clipboard:Clipboard = Toolkit.getDefaultToolkit.getSystemClipboard
      bufferedImage = Some(clipboard.getData(DataFlavor.imageFlavor).asInstanceOf[BufferedImage])
    } catch {
      // case java.awt.datatransfer.UnsupportedFlavorException: Image
      case e: Exception => println("CLIPBOARD ERROR: " + e.toString)
    }
  }

  def fromFile(f: File) = {
    try {
      bufferedImage = Some(ImageIO.read(f));
    } catch {
      case e: Exception => println("FILE ERROR: " + e.toString)
    }
  }

  def reset = { bufferedImage = None }
  def isPresent = bufferedImage.isDefined
  def save(f: File) = { bufferedImage.map(bim => ImageIO.write(bim, "jpg", f)) }

  def getImageView: ImageView = {
    if (bufferedImage.isDefined) {

      val g2d:Graphics2D = bufferedImage.get.createGraphics
      g2d.fillRect(0, 0, 200, 200)

      new ImageView { image = SwingFXUtils.toFXImage(bufferedImage.get, null) }
    } else {
      ContentNoImage.getImageView
    }
  }

  def get = new BorderPane {
    padding = Insets(10, 10, 10, 10)  
    style = "-fx-background-color: #822"
    center = new ScrollPane {
      content = getImageView
    }
  }

  fromClipboard
}
