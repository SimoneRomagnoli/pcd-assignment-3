package part1

import scalafx.application.JFXApp3
import scalafx.scene.Scene

object GraphicalUserInterface extends JFXApp3 {

  stage = new JFXApp3.PrimaryStage {
    title = "Actor-based Word Counter"
    scene = new Scene(800, 600)

  }

  override def start(): Unit = {

  }
}
