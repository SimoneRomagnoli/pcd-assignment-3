package part1

import javafx.beans.binding.Bindings
import part1.Messages.Parameters
import part1.WordCounterGUI.{ObservableOccurrences, excludedWordsFile, limitWords, occurrences, pdfDirectory, stage, wordCounter}
import scalafx.application.JFXApp
import scalafx.application.JFXApp.PrimaryStage
import scalafx.beans.property.StringProperty
import scalafx.collections.ObservableBuffer
import scalafx.scene.Scene
import scalafx.scene.control.{Button, Label, Spinner, SpinnerValueFactory, TableColumn, TableView, TextArea, TextField}
import scalafx.stage.{DirectoryChooser, FileChooser}

import java.io.File

object WordCounterGUI extends JFXApp {

  var pdfDirectory = "."
  var excludedWordsFile:File = null
  var limitWords = 5

  case class ObservableOccurrences(_word:String, var _occurrences:Int) {
    val word = new StringProperty(this, "word", _word)
    var occurrences = new StringProperty(this, "occurrences", _occurrences.toString)
  }

  val occurrences: ObservableBuffer[ObservableOccurrences] =
    new ObservableBuffer[ObservableOccurrences]()

  val wordCounter: WordCounter = WordCounter()

  stage = new PrimaryStage {
    val WIDTH:Double = 800
    val HEIGHT:Double = 600

    title = "Actor-based Word Counter"
    scene = WordCounterScene(WIDTH, HEIGHT)
    this.setWidth(WIDTH)
    this.setHeight(HEIGHT)

  }
}

case class WordCounterScene(WIDTH:Double, HEIGHT:Double) extends Scene {
  val update:String => Unit = text => outputTextArea.setText(text)

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
  content.add(directoryLabel)

  val directoryTextField = new TextField()
  directoryTextField.layoutX = WIDTH %% 5
  directoryTextField.layoutY = HEIGHT %% 10
  directoryTextField.setMinWidth(WIDTH %% 20)
  content.add(directoryTextField)

  val directoryButton = new Button("Browse")
  directoryButton.layoutX = WIDTH %% 5
  directoryButton.layoutY = HEIGHT %% 15
  directoryButton.onAction = _ => {
    val directoryChooser = new DirectoryChooser()
    directoryChooser.initialDirectory = new File(System.getProperty("user.dir"))
    directoryTextField.text = directoryChooser.showDialog(stage).getAbsolutePath
    pdfDirectory = directoryTextField.text.value
  }
  content.add(directoryButton)

  //EXCLUDED WORDS FILE
  val fileLabel = new Label("Choose the excluded words file:")
  fileLabel.layoutX = WIDTH %% 5
  fileLabel.layoutY = HEIGHT %% 35
  content.add(fileLabel)

  val fileTextField = new TextField()
  fileTextField.layoutX = WIDTH %% 5
  fileTextField.layoutY = HEIGHT %% 40
  fileTextField.setMinWidth(WIDTH %% 20)
  content.add(fileTextField)

  val fileButton = new Button("Browse")
  fileButton.layoutX = WIDTH %% 5
  fileButton.layoutY = HEIGHT %% 45
  fileButton.onAction = _ => {
    val fileChooser = new FileChooser()
    fileChooser.initialDirectory = new File(System.getProperty("user.dir"))
    excludedWordsFile = fileChooser.showOpenDialog(stage)
    fileTextField.text = excludedWordsFile.getAbsolutePath
  }
  content.add(fileButton)

  //LIMIT WORDS
  val limitLabel = new Label("Choose the excluded words file:")
  limitLabel.layoutX = WIDTH %% 5
  limitLabel.layoutY = HEIGHT %% 65
  content.add(limitLabel)

  val limitSpinner = new Spinner[Integer]
  limitSpinner.layoutX = WIDTH %% 5
  limitSpinner.layoutY = HEIGHT %% 70
  limitSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1,50, 5))
  limitSpinner.onMouseClicked = _ => {
    limitWords = limitSpinner.value.value
  }
  content.add(limitSpinner)

  //OUTPUT TABLE
  val tableOutput:TableView[ObservableOccurrences] = new TableView[ObservableOccurrences](occurrences) {
    columns ++= List(
      new TableColumn[ObservableOccurrences, String] {
        text = "Word"
        cellValueFactory = { _.value.word }
        prefWidth = WIDTH %% 20
      },
      new TableColumn[ObservableOccurrences, String] {
        text = "Occurrences"
        cellValueFactory = { _.value.occurrences }
        prefWidth = WIDTH %% 20

      }
    )
  }
  tableOutput.setFixedCellSize(25)
  tableOutput.prefHeightProperty().bind(Bindings.size(tableOutput.getItems).multiply(tableOutput.getFixedCellSize).add(30))
  tableOutput.layoutX = WIDTH %% 50
  tableOutput.layoutY = HEIGHT %% 5
  tableOutput.setMinWidth(WIDTH %% 40)
  tableOutput.setMaxHeight(HEIGHT %% 50)
  //content.add(tableOutput)

  //TEXT-AREA
  val outputTextArea = new TextArea()
  outputTextArea.layoutX = WIDTH %% 50
  outputTextArea.layoutY = HEIGHT %% 5
  outputTextArea.setMaxWidth(WIDTH %% 20)
  outputTextArea.setMaxHeight(HEIGHT %% 50)
  content.add(outputTextArea)

  //START-STOP BUTTONS
  val startButton = new Button("Start")
  startButton.layoutX = WIDTH %% 50
  startButton.layoutY = HEIGHT %% 80
  startButton.onAction = _ => {
    wordCounter ! Parameters(pdfDirectory, excludedWordsFile, limitWords, update)
  }
  content.add(startButton)

  content.forEach(el => el.setStyle("-fx-font: 12 arial;"))
}
