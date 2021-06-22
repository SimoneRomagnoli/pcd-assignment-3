package part1

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.text.PDFTextStripper
import part1.WordCounterGUI.ObservableOccurrences
import part1.Messages.{Directory, Parameters, Pdf, Text, Words}
import scalafx.collections.ObservableBuffer

import java.io.File
import java.nio.file.{Files, Paths}
import java.util.Scanner

object Messages {
  final case class Parameters(directoryPath:String, file:File, limit:Int)
  final case class Directory(path: String)
  final case class Pdf(document: PDDocument, title:String)
  final case class Text(text: String, title:String)
  final case class Words(words: List[Occurrences], title:String)
}

object Extractor {
  def apply(stripper:ActorRef[Pdf]): Behavior[Directory] = Behaviors.receive { (ctx, msg) =>
    ctx.log.info(s"Received message $msg")
    val directory = new File(msg.path)
    directory match {
      case Valid(directory) =>
        directory
          .listFiles(f => f.isFile && f.getName.endsWith(".pdf"))
          .foreach(file => stripper ! Pdf(PDDocument.load(file), file.getName))
        Behaviors.same
      case _ => ctx.log.info(s"Received message $msg which is not a directory")
        Behaviors.stopped
    }
  }
}

object Valid {
  def unapply(directory:File): Option[File] = if(directory.exists() && directory.isDirectory) Some(directory) else None
}

object Stripper {
  def apply(counter:ActorRef[Text]): Behavior[Pdf] = Behaviors.receive { (ctx, msg) =>
      ctx.log.info(s"Received document ${msg.title}")
      val child = ctx.spawn(StripBehavior(counter), msg.title)
      child ! msg
      Behaviors.same
  }
}

object StripBehavior {
  def apply(counter:ActorRef[Text]): Behavior[Pdf] = Behaviors.receive { (ctx, msg) =>
    ctx.log.info(s"Start stripping document ${msg.title}")
    val stripper = new PDFTextStripper
    stripper.setStartPage(1)
    stripper.setEndPage(msg.document.getNumberOfPages)
    stripper.setSortByPosition(true)
    val text = stripper.getText(msg.document)
    msg.document.close()
    ctx.log.info(s"Finished stripping document ${msg.title}")
    counter ! Text(text, msg.title)
    Behaviors.stopped
  }
}

object Counter {
  def apply(ignoredWords:List[String], gatherer: ActorRef[Words]): Behavior[Text] = Behaviors.receive { (ctx, msg) =>
    ctx.log.info(s"Received raw text of document ${msg.title}")
    val child = ctx.spawn(SplitFilterCountBehavior(ignoredWords, gatherer), msg.title)
    child ! msg
    Behaviors.same
  }
}

object SplitFilterCountBehavior {
  val REGEX = "[^a-zA-Z0-9]"

  def apply(ignoredWords:List[String], gatherer: ActorRef[Words]): Behavior[Text] = Behaviors.receive { (ctx, msg) =>
    ctx.log.info(s"Start processing raw text of document ${msg.title}")
    var occurrences: List[Occurrences] = List()

    for(word <- msg.text.split(REGEX).filter(word => !ignoredWords.contains(word))) {
      if(occurrences.exists(oc => oc.word == word)) {
        occurrences.filter(oc => oc.word == word).head.increment()
      } else {
        occurrences = Occurrences(word, 1) :: occurrences
      }
    }

    gatherer ! Words(occurrences, msg.title)

    Behaviors.stopped
  }
}

object Gatherer {
  var globalOccurrences:List[Occurrences] = List()

  def apply(globals:ObservableBuffer[ObservableOccurrences], limit:Int): Behavior[Words] = Behaviors.receive { (ctx, msg) =>
    ctx.log.info(s"Gathering words from file ${msg.title}")
    for(occurrence <- msg.words) {
      if(globalOccurrences.exists(go => go.word equals occurrence.word)) {
        val oldVal = globalOccurrences.filter(go => go.word equals occurrence.word).head.occurrences
        globalOccurrences = globalOccurrences updated(globalOccurrences.indexOf(Occurrences(occurrence.word, oldVal)), Occurrences(occurrence.word, oldVal+occurrence.occurrences))
      } else {
        globalOccurrences = globalOccurrences appended Occurrences(occurrence.word, occurrence.occurrences)
      }
    }

    globals.clear()
    globals.appendAll(
      globalOccurrences
        .sorted((a:Occurrences,b:Occurrences) => b.occurrences - a.occurrences)
        .map(go => ObservableOccurrences(go.word, go.occurrences))
        .take(limit)
    )

    Behaviors.same
  }
}

case class Occurrences(word:String, var occurrences:Int) {
  def increment(): Unit = occurrences += 1
}

case class WordCounter(occurrences: ObservableBuffer[ObservableOccurrences]) {
  val system:ActorSystem[Parameters] = ActorSystem[Parameters](Behaviors.setup { ctx =>

    Behaviors.receiveMessage {
      case msg if Files.exists(Paths.get(msg.directoryPath)) && Files.exists(Paths.get(msg.file.getAbsolutePath)) =>
        ctx.log.info(s"Received message $msg")

        val scanner = new Scanner(msg.file)
        var excludedWords:List[String] = List()

        while (scanner.hasNextLine) {
          excludedWords = excludedWords appended scanner.nextLine()
        }

        val gatherer = ctx.spawn(Gatherer(occurrences, msg.limit), "gatherer")
        val counter = ctx.spawn(Counter(excludedWords, gatherer), "counter")
        val stripper = ctx.spawn(Stripper(counter), "stripper")
        val extractor = ctx.spawn(Extractor(stripper), "extractor")

        extractor ! Directory(msg.directoryPath)
        Behaviors.same
      case _ =>
        ctx.log.info("Received non-valid msg")
        Behaviors.stopped
    }
  }, name = "actor-based-word-counter")

  def !(msg:Parameters): Unit = system ! msg
}