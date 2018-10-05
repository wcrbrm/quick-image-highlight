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
import scalafx.scene.input.MouseEvent
import scalafx.scene.control._
import scalafx.scene.control.ButtonBar.ButtonData
import scalafx.Includes._
import scalafx.event.{ ActionEvent, EventHandler }

object QuickImageHighlight extends JFXApp {

  def updateViews = {
    borderPane.top = topPanel.get
    borderPane.center = CurrentImage.get
  }

  val topPanel:TopPanel = new TopPanel(onUpdate = () => updateViews)
  val borderPane: BorderPane = new BorderPane {
    style = "-fx-background-color: #333"
    center = CurrentImage.get
    top = topPanel.get
  }

  CurrentImage.onUpdate = () => { borderPane.center = CurrentImage.get }

  stage = new PrimaryStage {
    title = "Please Select Image"
    scene = new Scene { root = borderPane }
  }
}
