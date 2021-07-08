package part1v2.controller
import part1v2.model.Messages.Parameters
import part1v2.model.WordCounter
import part1v2.view.View
import scala.jdk.CollectionConverters._

import java.io.File

case class Controller(view: View) extends InputListener {
  var wordCounter: WordCounter = WordCounter(this)
  val stopFlag:Flag= new Flag()


  override def started(dir: File, wordsFile: File, limitWords: Int): Unit = {
    wordCounter = WordCounter(this)
    stopFlag.reset()
    wordCounter ! Parameters(dir.getAbsolutePath, wordsFile, limitWords)
  }

  override def stopped(): Unit = {
    stopFlag.set();
  }

  def update(words: Int, map: Map[String, Integer]): Unit = {
    view.update(words, map.asJava)
  }
}
