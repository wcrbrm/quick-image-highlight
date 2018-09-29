package com.webcerebrium.quick_image

import scalafx.scene.text.Text
import scalafx.scene.image.{ Image, ImageView }
import scalafx.scene.layout.{ BorderPane, HBox, VBox }
import scalafx.geometry.{ Insets, Pos }
import scalafx.scene.paint.Color._
import scalafx.scene.paint._
import scalafx.Includes._

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