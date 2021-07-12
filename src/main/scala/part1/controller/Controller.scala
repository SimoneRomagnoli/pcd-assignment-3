package part1.controller
import part1.model.Messages.Parameters
import part1.model.WordCounter
import part1.view.View
import scala.jdk.CollectionConverters._

import java.io.File

/**
 * Connects View and Model.
 * Converts Scala structures into Java ones for the graphical user interface.
 *
 * @param view
 */
case class Controller(view: View) extends InputListener {
  var wordCounter: WordCounter = WordCounter(this)

  /**
   * Starts a new model by recreating the main actor system
   *
   * @param dir the directory containing pdfs
   * @param wordsFile the file containing the excluded words
   * @param limitWords the number of words to display
   */
  override def started(dir: File, wordsFile: File, limitWords: Int): Unit = {
    wordCounter = WordCounter(this)
    wordCounter ! Parameters(dir.getAbsolutePath, wordsFile, limitWords)
  }

  /**
   * Stops the program.
   *
   */
  override def stopped(): Unit = {
    wordCounter.stop()
  }

  /**
   * Updates the gui with the following parameters:
   *
   * @param words the number of elaborated words
   * @param map the occurrences of each word found in the pdf
   */
  def update(words: Int, map: Map[String, Integer]): Unit = {
    view.update(words, map.asJava)
  }

  /**
   * Warns the gui that the computation has finished.
   *
   */
  def done():Unit = {
    view.done()
  }
}
