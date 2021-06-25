package part1

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import part1.Messages.{Occurrences, Parameters}
import scalafx.application.JFXApp
import scalafx.application.JFXApp.PrimaryStage
import scalafx.scene.Scene
import scalafx.scene.control.{Button, DialogPane, Label, Spinner, SpinnerValueFactory, TextArea, TextField}
import scalafx.scene.layout.{BorderPane, FlowPane, GridPane, HBox, Pane, Region, StackPane, VBox}
import scalafx.stage.{DirectoryChooser, FileChooser}

import java.io.File

object AkkaWordCounter extends JFXApp {
  val WIDTH:Double = 800
  val HEIGHT:Double = 600

  object Gatherer {
    var globalOccurrences:Map[String, Int] = Map()

    def apply(limit:Int): Behavior[Occurrences] = Behaviors.receive { (_, msg) =>
      for ((k, v) <- msg.occurrences) {
        if (globalOccurrences.contains(k)) {
          globalOccurrences = globalOccurrences + (k -> (globalOccurrences(k) + v))
        } else {
          globalOccurrences = globalOccurrences + (k -> v)
        }
      }

      outputTextArea.text = globalOccurrences
        .keySet
        .toList
        .sorted((a: String, b: String) => globalOccurrences(b) - globalOccurrences(a))
        .take(limitSpinner.value.value)
        .map(s => s + " - " + globalOccurrences(s))
        .reduce((a: String, b: String) => a + "\n" + b)
      Behaviors.same
    }
  }

  val system:WordCounterActorSystem = WordCounterActorSystem()

  object Positions {
    implicit class Position(value:Double) {
      def %%(p:Double): Double = value * p / 100
    }
  }

  import Positions._

  //PDF DIRECTORY
  val directoryLabel = new Label("Choose the pdf directory:")
  directoryLabel.layoutX = WIDTH %% 5
  directoryLabel.layoutY = HEIGHT %% 5
  directoryLabel.setStyle("-fx-font: 12 arial;")

  val directoryTextField = new TextField()
  directoryTextField.layoutX = WIDTH %% 5
  directoryTextField.layoutY = HEIGHT %% 10
  directoryTextField.setMinWidth(WIDTH %% 20)
  directoryTextField.setStyle("-fx-font: 12 arial;")

  val directoryButton = new Button("Browse")
  directoryButton.layoutX = WIDTH %% 5
  directoryButton.layoutY = HEIGHT %% 15
  directoryButton.onAction = _ => {
    val directoryChooser = new DirectoryChooser()
    directoryChooser.initialDirectory = new File(System.getProperty("user.dir"))
    directoryTextField.text = directoryChooser.showDialog(stage).getAbsolutePath
  }
  directoryButton.setStyle("-fx-font: 12 arial;")

  //EXCLUDED WORDS FILE
  val fileLabel = new Label("Choose the excluded words file:")
  fileLabel.layoutX = WIDTH %% 5
  fileLabel.layoutY = HEIGHT %% 35
  fileLabel.setStyle("-fx-font: 12 arial;")

  val fileTextField = new TextField()
  fileTextField.layoutX = WIDTH %% 5
  fileTextField.layoutY = HEIGHT %% 40
  fileTextField.setMinWidth(WIDTH %% 20)
  fileTextField.setStyle("-fx-font: 12 arial;")

  val fileButton = new Button("Browse")
  fileButton.layoutX = WIDTH %% 5
  fileButton.layoutY = HEIGHT %% 45
  fileButton.onAction = _ => {
    val fileChooser = new FileChooser()
    fileChooser.initialDirectory = new File(System.getProperty("user.dir"))
    fileTextField.text = fileChooser.showOpenDialog(stage).getAbsolutePath
  }
  fileButton.setStyle("-fx-font: 12 arial;")

  //LIMIT WORDS
  val limitLabel = new Label("Choose the excluded words file:")
  limitLabel.layoutX = WIDTH %% 5
  limitLabel.layoutY = HEIGHT %% 65
  limitLabel.setStyle("-fx-font: 12 arial;")

  val limitSpinner = new Spinner[Integer]
  limitSpinner.layoutX = WIDTH %% 5
  limitSpinner.layoutY = HEIGHT %% 70
  limitSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1,50, 5))
  limitSpinner.setStyle("-fx-font: 12 arial;")

  //TEXT-AREA
  val outputTextArea = new TextArea()
  outputTextArea.layoutX = WIDTH %% 50
  outputTextArea.layoutY = HEIGHT %% 5
  outputTextArea.setMaxWidth(WIDTH %% 20)
  outputTextArea.setMaxHeight(HEIGHT %% 50)
  outputTextArea.setStyle("-fx-font: 12 arial;")

  //START-STOP BUTTONS
  val startButton = new Button("Start")
  startButton.layoutX = WIDTH %% 50
  startButton.layoutY = HEIGHT %% 80
  startButton.setStyle("-fx-font: 12 arial;")
  startButton.onAction = _ => {
    system ! Parameters(directoryTextField.text.value, new File(fileTextField.text.value), limitSpinner.value.value)
    println("start")
  }

  val pane = new Pane()
  pane.getChildren.addAll(directoryLabel, directoryButton,
    directoryTextField, fileLabel, fileTextField, fileButton,
    limitLabel, limitSpinner, outputTextArea, startButton)

  stage = new PrimaryStage {
    val WIDTH:Double = 800
    val HEIGHT:Double = 600

    title = "Actor-based Word Counter"
    scene = new Scene {
      root = pane
    }
    this.setWidth(WIDTH)
    this.setHeight(HEIGHT)
  }

}
