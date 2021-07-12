package part1.model

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Behavior, Terminated}
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.text.PDFTextStripper
import part1.controller.Controller

import java.io.File
import java.nio.file.{Files, Paths}
import java.util.Scanner
import part1.model.Messages._

/**
 * Object containing messages of the actor system.
 *
 */
object Messages {
  sealed trait Input
  final case class Parameters(directoryPath:String, file:File, limit:Int) extends Input
  final case class Directory(path: String)
  final case class Pdf(document: PDDocument, title:String)
  final case class Text(text: String, title:String)

  sealed trait Output
  final case class Pages(numberOfPages:Int) extends Output
  final case class Occurrences(occurrences: Map[String, Int], words:Int, title:String) extends Output
}

/**
 * Extractor behavior:
 *    extracts pdf documents from a directory
 *    and sends them to the next actor.
 *
 */
object Extractor {
  def apply(stripper:ActorRef[Pdf], gatherer:ActorRef[Output]): Behavior[Directory] = Behaviors.receive { (ctx, msg) =>
    ctx.log.info(s"Received message $msg")
    val directory = new File(msg.path)
    directory match {
      case Valid(directory) =>
        directory
          .listFiles(f => f.isFile && f.getName.endsWith(".pdf"))
          .foreach(file => {
            val pdf = PDDocument.load(file)
            gatherer ! Pages(pdf.getNumberOfPages)
            stripper ! Pdf(pdf, file.getName)
          })
        Behaviors.stopped
      case _ => ctx.log.info(s"Received message $msg which is not a directory")
        Behaviors.stopped
    }
  }
}

object Valid {
  def unapply(directory:File): Option[File] = if(directory.exists() && directory.isDirectory) Some(directory) else None
}

/**
 * Stripper behavior:
 *    spawns a stripper-child
 *    and turns back to listening.
 *
 */
object Stripper {
  def apply(counter:ActorRef[Text]): Behavior[Pdf] = {
    Behaviors.receive { (ctx, msg) =>
      ctx.log.info(s"Received document ${msg.title}")
      val child = ctx.spawn(StripperChild(counter), msg.title)
      child ! msg
      Behaviors.same
    }
  }
}

/**
 * StripperChild behavior:
 *    strips a pdf document
 *    and sends its text to the next actor.
 *
 */
object StripperChild {
  def apply(counter:ActorRef[Text]): Behavior[Pdf] = Behaviors.receive { (ctx, msg) =>
    ctx.log.info(s"Start stripping document ${msg.title}")
    val stripper = new PDFTextStripper

    for(i <- 1 to msg.document.getNumberOfPages) {
      stripper.setStartPage(i)
      stripper.setEndPage(i)
      stripper.setSortByPosition(true)
      counter ! Text(stripper.getText(msg.document), msg.title+" - "+i)
    }
    msg.document.close()
    ctx.log.info(s"Finished stripping document ${msg.title}")
    Behaviors.stopped
  }
}

/**
 * Counter behavior:
 *    splits words in a text, filters them and counts occurrences for each word,
 *    sends the occurrences to the next actor
 *    and turns back to listening.
 *
 */
object Counter {
  val REGEX = "[^a-zA-Z0-9]"

  def apply(ignoredWords:List[String], gatherer: ActorRef[Occurrences]): Behavior[Text] = Behaviors.receive { (ctx, msg) =>
    ctx.log.info(s"Start processing raw text of document ${msg.title}")
    var occurrences: Map[String, Int] = Map()
    for(word <- msg.text.split(REGEX).filter(word => !ignoredWords.contains(word))) {
      if(occurrences.contains(word)) {
        occurrences = occurrences + (word -> (occurrences(word)+1))
      } else {
        occurrences = occurrences + (word -> 1)
      }
    }
    gatherer ! Occurrences(occurrences, msg.text.split(REGEX).length, msg.title)
    Behaviors.same
  }
}

/**
 * Gatherer behavior:
 *    gathers occurrences from the counter actor,
 *    updates the gui
 *    and turns back to listening.
 *
 */
object Gatherer {
  var globalOccurrences:Map[String, Int] = Map()
  var elaboratedWords:Int = 0
  var totalPages:Int = 0
  var receivedMessages:Int = 0

  def apply(limit: Int, controller: Controller): Behavior[Output] = {
    globalOccurrences = Map()
    elaboratedWords = 0
    totalPages = 0
    receivedMessages = 0

    Behaviors.receiveMessage {
      case Pages(numberOfPages) =>
        totalPages += numberOfPages
        Behaviors.same
      case Occurrences(occurrences, words, _) =>
        receivedMessages += 1
        elaboratedWords += words
        for((k,v) <- occurrences) {
          if (globalOccurrences.contains(k)) {
            globalOccurrences = globalOccurrences + (k -> (globalOccurrences(k) + v))
          } else {
            globalOccurrences = globalOccurrences + (k -> v)
          }
        }
        controller.update(elaboratedWords, globalOccurrences
          .keySet
          .toList
          .sorted((a:String,b:String) => globalOccurrences(b) - globalOccurrences(a))
          .take(limit)
          .map((k:String) => k -> Integer.valueOf(globalOccurrences(k))).toMap)
        if(receivedMessages >= totalPages) {
          Behaviors.stopped
        } else {
          Behaviors.same
        }
    }
  }
}

/**
 * Class containing the Actor System of the application.
 * Receives input messages from the user interface (START and STOP buttons).
 *
 * @param controller the application controller, needed to update the gui
 */
case class WordCounter(controller: Controller) {
  val system:ActorSystem[Input] = ActorSystem[Input](Behaviors.setup { ctx =>
    Behaviors.receiveMessage[Input] {
      case Parameters(directoryPath, file, limit) if Files.exists(Paths.get(directoryPath)) && Files.exists(Paths.get(file.getAbsolutePath)) =>
        val scanner = new Scanner(file)
        var excludedWords:List[String] = List()
        while (scanner.hasNextLine) {
          excludedWords = excludedWords appended scanner.nextLine()
        }

        val gatherer = ctx.spawn(Gatherer(limit, controller), "gatherer")
        val counter = ctx.spawn(Counter(excludedWords, gatherer), "counter")
        val stripper = ctx.spawn(Stripper(counter), "stripper")
        val extractor = ctx.spawn(Extractor(stripper, gatherer), "extractor")

        ctx.watch(gatherer)

        extractor ! Directory(directoryPath)
        Behaviors.same
    }.receiveSignal {
      case (_, Terminated(_)) =>
        controller.done()
        Behaviors.stopped
    }
  }, name = "actor-based-word-counter")

  def !(msg:Parameters):Unit = system ! msg

  def stop():Unit = system.terminate()
}