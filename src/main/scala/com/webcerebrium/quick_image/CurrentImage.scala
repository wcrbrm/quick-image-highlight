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

import java.awt.{Toolkit, Graphics2D, RenderingHints, BasicStroke, Polygon}
import java.awt.datatransfer.{ Clipboard, Transferable, DataFlavor, UnsupportedFlavorException }
import java.awt.image.BufferedImage
import java.awt.geom._
import scalafx.embed.swing.SwingFXUtils 

import java.io.{File, IOException }
import java.net.URL
import javax.imageio.ImageIO

// https://www.scala-lang.org/old/node/10356.html
// https://github.com/holgerbrandl/pasteimages/blob/master/src/img2md/ImageUtils.java

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

  def drawArrow(g2d: Graphics2D, x1: Int, y1: Int, x2: Int, y2: Int ):Unit = {
    val arrowPolygon = new Polygon
    arrowPolygon.addPoint(-12,1)
    arrowPolygon.addPoint(3,1)
    arrowPolygon.addPoint(3,3)
    arrowPolygon.addPoint(12,0)
    arrowPolygon.addPoint(3,-3)
    arrowPolygon.addPoint(3,-1)
    arrowPolygon.addPoint(-12,-1)

    val midPoint = new Point2D((x1 + x2) / 2, (y1 + y2) / 2)
    val rotate = Math.atan2(y2 - y1, x2 - x1);

    val tx = new AffineTransform
    // tx.setToIdentity
    tx.translate(midPoint.getX, midPoint.getY);

    val ptDistance = Math.sqrt( (x2 - x1)*(x2 - x1) + (y2 - y1) * (y2 - y1))
    val scale = ptDistance / 24.0; // 12 because it's the length of the arrow polygon.
    tx.scale(scale, scale);
    tx.rotate(rotate);

    val shape = tx.createTransformedShape(arrowPolygon);
    g2d.fill(shape)
  }

  def saveToClipboard = {
    bufferedImage.map(bim => {
      
      val clipboard:Clipboard = Toolkit.getDefaultToolkit.getSystemClipboard
      clipboard.setContents(new Transferable {
		    override def isDataFlavorSupported(flavor: DataFlavor):Boolean = {
			    flavor.equals(DataFlavor.imageFlavor);
		    }
		    override def getTransferDataFlavors: Array[DataFlavor]= {
			    Array(DataFlavor.imageFlavor)
		    }
				override def getTransferData(flavor:DataFlavor) = { // throws UnsupportedFlavorException, IOException 
			    if (flavor == DataFlavor.imageFlavor) {
				    bim
			    } else {
			      throw new UnsupportedFlavorException(flavor)
          }
		    }
	    }, null );
    })
  }

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
        drawArrow(g2d, p1.getX.toInt, p1.getY.toInt, p2.getX.toInt, p2.getY.toInt)
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
      content = if (bufferedImage.isDefined) getImageView else ContentNoImage.get
    }
  }

  fromClipboard
}
