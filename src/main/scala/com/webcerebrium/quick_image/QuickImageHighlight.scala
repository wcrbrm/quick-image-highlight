package com.webcerebrium.quick_image

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
import scalafx.scene.input.{ KeyEvent, MouseEvent }
import scalafx.scene.control._
import scalafx.scene.control.ButtonBar.ButtonData
import scalafx.Includes._
import scalafx.event.{ ActionEvent, EventHandler }
import javafx.scene.{control => jfxctrl, input => jfxsi, layout => jfxsl, paint => jfxsp}
import javafx.{collections => jfxc, event => jfxe, geometry => jfxg, scene => jfxs, util => jfxu}

object QuickImageHighlight extends JFXApp {

  def updateViews = {
    this.stage.title = if (CurrentImage.bufferedImage.isDefined) "Highlight the most important" else "Please Select Image"
    borderPane.top = topPanel.get
    borderPane.center = CurrentImage.get
  }

  CurrentImage.onUpdate = () => { borderPane.center = CurrentImage.get }
  val topPanel:TopPanel = new TopPanel(
     onUpdate = () => updateViews,
     onUpdateMode = (mode: String) => { CurrentImage.mode = mode; println("mode updated " + mode) }
  )
  val borderPane: BorderPane = new BorderPane {
    style = "-fx-background-color: #333"
    center = CurrentImage.get
    top = topPanel.get
  }

  def selectMode(modeIndex: Int) = {
    topPanel.getModeButton(modeIndex).map(b => {
      CurrentImage.mode = b.getText
      b.selected = true
      b.requestFocus
    })
  }

  def triggerButton(name: String) = {
    topPanel.getButton(name).map(b => {
      println("button name: " + name)
      b.requestFocus
      b.fire
    })
  }

  def onKeyPress(ke: jfxsi.KeyEvent) = {
    if (ke.getCode == jfxsi.KeyCode.DIGIT1) {
      selectMode(0);  
    } else if (ke.getCode == jfxsi.KeyCode.DIGIT2) {
      selectMode(1);  
    } else if (ke.getCode == jfxsi.KeyCode.DIGIT3) {
      selectMode(2);  
    } else if (ke.getCode == jfxsi.KeyCode.DIGIT4) {
      selectMode(3);
    } else if (ke.getCode == jfxsi.KeyCode.ESCAPE) {  
      triggerButton("RESET")
    } else if (ke.getCode == jfxsi.KeyCode.V && ke.isControlDown) {  
      triggerButton("PASTE")
    } else if (ke.getCode == jfxsi.KeyCode.O && ke.isControlDown) {  
      triggerButton("OPEN")
    } else if (ke.getCode == jfxsi.KeyCode.C && ke.isControlDown) {  
      triggerButton("COPY")
    } else if (ke.getCode == jfxsi.KeyCode.S && ke.isControlDown) {  
      triggerButton("SAVE")
    } else if (ke.getCode == jfxsi.KeyCode.ENTER && ke.isControlDown) {  
      triggerButton("SHARE")
    } else {
      println(ke.toString)
    }
  }


  stage = new PrimaryStage {
    icons += new Image("/favicon.png")
    title = "Please Select Image"
    scene = new Scene { 
      root = borderPane
      alwaysOnTop = true
      onKeyPressed = new jfxe.EventHandler[jfxsi.KeyEvent] {
        override def handle(ke: jfxsi.KeyEvent) { onKeyPress(ke) }
      }
    }
  }

  
}
