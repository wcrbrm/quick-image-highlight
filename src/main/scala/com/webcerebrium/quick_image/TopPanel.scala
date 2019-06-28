package com.webcerebrium.quick_image

import scalafx.geometry.{ Insets, Pos }
import scalafx.scene.paint.Color._
import scalafx.scene.paint._
import scalafx.scene.text.Text
import scalafx.scene.image.{Image, ImageView}
import scalafx.scene.layout.{ FlowPane, BorderPane, HBox, VBox }
import scalafx.event.{ ActionEvent, EventHandler }
import scalafx.scene.control.Alert.AlertType
import scalafx.scene.control._
import scalafx.Includes._

import java.awt.{Toolkit}
import java.awt.datatransfer.{ Clipboard, DataFlavor, UnsupportedFlavorException }

import java.io.File
import java.nio.file.{Files, Paths}
import javafx.scene.{control => jfxsc, text => jfxst}
import javafx.scene.control.{ToggleButton => JfxToggleBtn}
import javafx.{scene => jfxs}

import scala.util.Properties.envOrElse
import fr.janalyse.ssh._

trait TopPanelTrait {
  def get:BorderPane
  def getButtonByName(name: String): Option[Button]
}

case class ModeText(value: String) extends Label(value) {
  style = "-fx-pref-width: 16px; -fx-font: normal bold 16pt sans-serif"
  padding = Insets(0, 2, 0, 6)
  textFill = new LinearGradient(endX = 0,stops = Stops(Cyan, DarkBlue))
}

case class TopPanelNoImage(onUpdate: () => Unit) extends TopPanelTrait with ImageChoser {

  val btnPaste = new Button("Clipboard") { 
    graphic = new ImageView {image = new Image(this, "/paste.png")}
    onAction = handle { 
        CurrentImage.fromClipboard 
        onUpdate()
    }
  }

  def hasPicturesFolder: Boolean = {
    val userFolder = System.getProperty("user.home")
    val dir = userFolder + "/Pictures"
    val d = new File(dir)
    d.exists && d.isDirectory
  }

  def getLatestFileInPictures: Option[File] = {
    val userFolder = System.getProperty("user.home")
    val dir = userFolder + "/Pictures"
    val d = new File(dir)
    val files = d.listFiles.filter(_.isFile).toList
      .sortWith((f1: File, f2: File) => {
        if (f1.lastModified - f2.lastModified > 0) true else false
      })
    println(files.length + " total files in " + dir)
    if (files.length > 0) Some(files.head) else None
  }

  val optBtnOpenPictures: Option[Button] = if (!hasPicturesFolder) {
    println("Warning: User Pictures folder not found, disabling this feature")
    None
  } else {
    Some(new Button("Pictures") {
      graphic = new ImageView {image = new Image(this, "/open-folder.png")}
      onAction = handle {
        val optFile = getLatestFileInPictures
        if (optFile.isDefined) {
          println("picking " + optFile.get.toString)
          CurrentImage.fromFile(optFile.get)
          onUpdate()
        } else {
          println("File not selected")
        }
      }
    })
  }  

  val btnOpenDialog = new Button("Local File") {
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
    } else if (name == "PICTURES") {
      optBtnOpenPictures
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
        children = List(optBtnOpenPictures).flatten[Button] ::: List(btnOpenDialog, btnPaste)
    }
  }
}

case class TopPanelWithImage(onUpdate: () => Unit, onUpdateMode: (String) => Unit) extends TopPanelTrait with ImageChoser {

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
      val imageFile: Option[File] = exportedImage("Save as...")
      if (imageFile.isDefined) {
        CurrentImage.save(imageFile.get)
        println("Consider All Saved") 
      } else {
        println("Exported file not selected") 
      }
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
    onAction = handle { 
       val QIH_HTTP_ROOT = envOrElse("QIH_HTTP_ROOT", "")
       val QIH_REMOTE_DIR = envOrElse("QIH_REMOTE_DIR", "")
       val QIH_HOST = envOrElse("QIH_HOST", "")
       val QIH_USERNAME = envOrElse("QIH_USERNAME", "root")
       val QIH_PASSWORD = envOrElse("QIH_PASSWORD", "")

      if (QIH_HTTP_ROOT.isEmpty || QIH_REMOTE_DIR.isEmpty || QIH_HOST.isEmpty ) {
        new Alert(AlertType.Error) {
          initOwner(null)
          title = "Not Configured"
          headerText = "Using Sharing function requires env variables"
          contentText = "please check\nQIH_HTTP_ROOT,\nQIH_REMOTE_DIR,\nQIH_HOST,\nQIH_USERNAME,\nQIH_PASSWORD"
        }.showAndWait
      } else {

        val basename = System.currentTimeMillis + ".png"
        val remoteFile = QIH_REMOTE_DIR + "/" + basename
        val link = QIH_HTTP_ROOT + basename
        val sshOptions = SSHOptions(host = QIH_HOST, username = QIH_USERNAME, password = QIH_PASSWORD )
        println("Connecting to ssh.. " + QIH_USERNAME + "@" + QIH_HOST)
        SSH.shellAndFtp(sshOptions) {(ssh, ftp) => {
          println("Connected to " + QIH_HOST)
          CurrentImage.upload(ftp, remoteFile)
          println("Image was uploaded. Link: " + link)

          // saving link to the clipboard
          import javafx.scene.input.{ Clipboard, ClipboardContent }
          val selection = new ClipboardContent
          selection.putString(link)
          Clipboard.getSystemClipboard.setContent(selection)
          println("Copied to clipboard")

          // TODO: display modal dialog with a link
        }}
      }
    }
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
      children = List( 
        ModeText("1"), btnCrop, 
        ModeText("2"), btnLine, 
        ModeText("3"), btnArrow, 
        ModeText("4"), btnBox
      )
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