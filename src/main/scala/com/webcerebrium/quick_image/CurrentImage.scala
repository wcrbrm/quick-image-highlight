package com.webcerebrium.quick_image

import scala.util.Try
import scalafx.geometry.{ Insets, Pos }
import scalafx.scene.image.{ Image, ImageView }
import scalafx.scene.layout.{ BorderPane, HBox, VBox }
import scalafx.scene.control._
import scalafx.scene.paint.Color._
import scalafx.scene.paint._

import javafx.geometry.Point2D
import javafx.scene.input.MouseEvent
import javafx.event.EventHandler

import java.awt.{Toolkit, Graphics2D, RenderingHints, BasicStroke}
import java.awt.datatransfer.{ Clipboard, DataFlavor, UnsupportedFlavorException }
import java.awt.image.BufferedImage
import scalafx.embed.swing.SwingFXUtils 

import java.io.{File, IOException }
import java.net.URL
import javax.imageio.ImageIO

object CurrentImage {

  var bufferedImage: Option[BufferedImage] = None
  var startPoint: Option[Point2D] = None
  var mode: String = "CROP"
  var onUpdate: () => Unit = () => {}

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

  def applyTool(p1: Point2D, p2: Point2D) = {
     val minX = Math.min(p1.getX, p2.getX).toInt
     val minY = Math.min(p1.getY, p2.getY).toInt
     val maxX = Math.max(p1.getX, p2.getX).toInt
     val maxY = Math.max(p1.getY, p2.getY).toInt

     println("Apply tool: " + mode + ", p1=" + p1.toString + ", p2=" + p2.toString)
     
     val g2d:Graphics2D = bufferedImage.get.createGraphics
     g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
     g2d.setColor(java.awt.Color.red)
     g2d.setStroke(new BasicStroke(3));

     if (mode == "CROP") {
        bufferedImage = Some(bufferedImage.get.getSubimage(minX, minY, maxX - minY, maxY - minY)) 
     } else if (mode == "LINE") {
        g2d.drawLine(p1.getX.toInt, p1.getY.toInt, p2.getX.toInt, p2.getY.toInt)
     } else if (mode == "ARROW") {
        // TODO:
     } else if (mode == "BOX") {
        g2d.drawRect(minX, minY, maxX - minX + 1, maxY - minY + 1)
     }
     
  }

  def getImageView: ImageView = {
    if (bufferedImage.isDefined) {

      // val g2d:Graphics2D = bufferedImage.get.createGraphics
      // g2d.fillRect(0, 0, 200, 200)

      new ImageView { 
        image = SwingFXUtils.toFXImage(bufferedImage.get, null) 
      
        onMouseClicked = new EventHandler[MouseEvent] {
	        override def handle(event: MouseEvent) {
            if (startPoint.isDefined) {
              applyTool(startPoint.get, new Point2D(event.getX, event.getY))
              startPoint = None;
            } else {
              startPoint = Some(new Point2D(event.getX, event.getY))
              println("startPoint = ", startPoint.toString)
            }
            onUpdate()
	        }
	      }
      }

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
