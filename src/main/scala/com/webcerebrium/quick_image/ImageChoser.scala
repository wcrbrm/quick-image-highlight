package com.webcerebrium.quick_image
import scalafx.stage.FileChooser
import java.io.File

trait ImageChoser {

  def selectImage(title: String = ""): Option[File] = {  
    val chooser = new FileChooser
    chooser.title = title
    chooser.getExtensionFilters.addAll(
      new FileChooser.ExtensionFilter("All Images", Seq("*.jpg", "*.png")),
      new FileChooser.ExtensionFilter("JPG", "*.jpg"),
      new FileChooser.ExtensionFilter("PNG", "*.png")
    )
    val result = chooser.showOpenDialog(null)
    println("Selected File: " + result)
    if (result != null) Some(result) else None
  }
  
  def exportedImage( title: String = ""): Option[File] = {  
    val chooser = new FileChooser
    chooser.title = title
    chooser.getExtensionFilters.addAll(
      new FileChooser.ExtensionFilter("JPG", "*.jpg"),
      new FileChooser.ExtensionFilter("PNG", "*.png")
    )
    val result = chooser.showSaveDialog(null)
    println("Selected File: " + result)
    if (result != null) Some(result) else None
  }

}