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

import java.io.{File, IOException, ByteArrayInputStream, ByteArrayOutputStream }
import java.net.URL
import fr.janalyse.ssh._
import javax.imageio.{ ImageIO, IIOImage, ImageWriter, ImageWriteParam }

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

  def rmAlpha(img: BufferedImage): BufferedImage = {
    val copy = new BufferedImage(
      img.getWidth, img.getHeight, BufferedImage.TYPE_INT_RGB);
    val g2d = copy.createGraphics
    g2d.setColor(java.awt.Color.BLACK)
    g2d.fillRect(0, 0, copy.getWidth, copy.getHeight)
    g2d.drawImage(img, 0, 0, null)
    g2d.dispose
    copy
  }

  def reset = { bufferedImage = None }
  def isPresent = bufferedImage.isDefined
  def save(f: File) = {
    bufferedImage.map(rmAlpha).map(bim => ImageIO.write(bim, "jpg", f)) 
  }

  def upload(sftp: SSHFtp, remoteDestination: String) = {
    bufferedImage.map(rmAlpha).map(bim => {
      val writer: ImageWriter = ImageIO.getImageWritersByFormatName("png").next
      val param:ImageWriteParam = writer.getDefaultWriteParam
      // param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT)
      // param.setCompressionQuality(0.9f)

      val os = new ByteArrayOutputStream
      val ios = ImageIO.createImageOutputStream(os)
      writer.setOutput(ios)
      writer.write(null, new IIOImage(bim, null, null), param);
      writer.dispose

      println("image writer done, length=" + os.toByteArray.length)
      println("output stream initialized")
      sftp.putFromStream(new ByteArrayInputStream(os.toByteArray), remoteDestination)
      println("sftp stream set up")

      os.flush
      os.close
    })
  }

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
        bufferedImage = Some(bufferedImage.get.getSubimage(minX, minY, maxX - minX, maxY - minY)) 
     } else if (mode == "LINE") {
        g2d.drawLine(p1.getX.toInt, p1.getY.toInt, p2.getX.toInt, p2.getY.toInt)
     } else if (mode == "ARROW") {
        drawArrow(g2d, p1.getX.toInt, p1.getY.toInt, p2.getX.toInt, p2.getY.toInt)
     } else if (mode == "BOX") {
        g2d.drawRect(minX, minY, maxX - minX + 1, maxY - minY + 1)
     }
     
  }

  var imageView: Option[ImageView] = None
  def updateCanvas = {
    imageView.get.image = SwingFXUtils.toFXImage(bufferedImage.get, null) 
  }

  def getImageView: ImageView = {
    if (bufferedImage.isDefined) {

      imageView = Some(new ImageView { 
        image = SwingFXUtils.toFXImage(bufferedImage.get, null) 
        onMouseMoved = new EventHandler[MouseEvent] {
          override def handle(event: MouseEvent) {
            if (startPoint.isDefined) {
              val from:Point2D = startPoint.get
              val to:Point2D = new Point2D(event.getX, event.getY)
              
              val b = new BufferedImage(bufferedImage.get.getWidth, bufferedImage.get.getHeight, bufferedImage.get.getType)
              val g = b.createGraphics
              g.drawImage(bufferedImage.get, 0, 0, null)

              g.setColor(java.awt.Color.GRAY);
              g.setStroke(new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, Array[Float](4), 0))
              if (mode == "CROP" || mode == "BOX") {
                g.drawRect(from.getX.toInt, from.getY.toInt, to.getX.toInt - from.getX.toInt + 1, to.getY.toInt - from.getY.toInt + 1)
              } else {
                g.drawLine(from.getX.toInt, from.getY.toInt, to.getX.toInt, to.getY.toInt)
              }
              g.dispose

              imageView.get.image = SwingFXUtils.toFXImage(b, null) 
            } 
          }
        }
        onMouseClicked = new EventHandler[MouseEvent] {
	        override def handle(event: MouseEvent) {
            if (startPoint.isDefined) {
              applyTool(startPoint.get, new Point2D(event.getX, event.getY))
              startPoint = None
            } else {
              startPoint = Some(new Point2D(event.getX, event.getY))
              println("startPoint = ", startPoint.toString)
            }
            updateCanvas
	        }
	      }
      })

      imageView.get
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
