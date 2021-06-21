package part1

import javafx.event.ActionEvent
import scalafx.application.{JFXApp, JFXApp3}
import scalafx.beans.property.StringProperty
import scalafx.collections.ObservableBuffer
import scalafx.scene.Scene
import scalafx.scene.control.{Button, Label, Spinner, SpinnerValueFactory, TableColumn, TableView, TextField}
import scalafx.stage.{DirectoryChooser, FileChooser}

import java.io.File


object GraphicalUserInterface extends JFXApp {

  val WIDTH:Double = 800
  val HEIGHT:Double = 600

  object Positions {
    implicit class Position(value:Double) {
      def %%(p:Double): Double = value * p / 100
    }
  }

  var pdfDirectory = "."
  var excludedWordsFile:File = null
  var limitWords = 5

  val wordCounter: WordCounter = WordCounter()

  case class ObservableOccurrences(_word:String, _occurrences:Int) {
    val word = new StringProperty(this, "word", _word)
    val occurrences = new StringProperty(this, "occurrences", _occurrences.toString)
  }

  val occurrences: ObservableBuffer[ObservableOccurrences] = null

  stage = new JFXApp.PrimaryStage {
    title = "Actor-based Word Counter"
    scene = new Scene(WIDTH,HEIGHT) {
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
      directoryButton.onAction = (e:ActionEvent) => {
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
      fileButton.onAction = (e:ActionEvent) => {
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
      val tableOutput = new TableView[ObservableOccurrences](occurrences) {
        columns ++= List(
          new TableColumn[ObservableOccurrences, String] {
            text = "Word"
            cellValueFactory = { _.value.word }
            prefWidth = 100
          },
          new TableColumn[ObservableOccurrences, String] {
            text = "Occurrences"
            cellValueFactory = { _.value.occurrences }
            prefWidth = 100
          }
        )
      }
      tableOutput.layoutX = WIDTH %% 50
      tableOutput.layoutY = HEIGHT %% 5
      tableOutput.setMinWidth(WIDTH %% 40)
      tableOutput.setMinHeight(HEIGHT %% 40)
      content.add(tableOutput)

      //START-STOP BUTTONS
      val startButton = new Button("Start")
      startButton.layoutX = WIDTH %% 50
      startButton.layoutY = HEIGHT %% 80
      startButton.onAction = (e:ActionEvent) => {
        wordCounter.system ! pdfDirectory
      }
      content.add(startButton)
    }
  }

}
