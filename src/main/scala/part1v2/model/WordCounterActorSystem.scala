package part1v2.model

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.text.PDFTextStripper
import part1v2.controller.Controller

import java.io.File
import java.nio.file.{Files, Paths}
import java.util.Scanner
import java.util.stream.Collectors
import part1v2.model.Messages._

object Messages {
  final case class Parameters(directoryPath:String, file:File, limit:Int)
  final case class Directory(path: String)
  final case class Pdf(document: PDDocument, title:String)
  final case class Text(text: String, title:String)
  final case class Occurrences(words: Map[String, Int], title:String)
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
    gatherer ! Occurrences(occurrences, msg.title)
    Behaviors.same
  }
}

object Gatherer {
  var globalOccurrences:Map[String, Int] = Map();

  def apply(limit: Int, controller: Controller): Behavior[Occurrences] = Behaviors.receive { (ctx, msg) =>
    ctx.log.info(s"Gathering words from file ${msg.title}")
    for((k,v) <- msg.words) {
      if (globalOccurrences.contains(k)) {
        globalOccurrences = globalOccurrences + (k -> (globalOccurrences(k) + v))
      } else {
        globalOccurrences = globalOccurrences + (k -> v)
      }
    }
      controller.update(100,globalOccurrences
        .keySet
        .toList
        .sorted((a:String,b:String) => globalOccurrences(b) - globalOccurrences(a))
        .take(limit)
        .map((k:String) => k -> Integer.valueOf(globalOccurrences(k))).toMap);
    Behaviors.same
    }

}

case class WordCounter(controller: Controller) {
  val system:ActorSystem[Parameters] = ActorSystem[Parameters](Behaviors.setup { ctx =>

    Behaviors.receiveMessage {
      case msg if Files.exists(Paths.get(msg.directoryPath)) && Files.exists(Paths.get(msg.file.getAbsolutePath)) =>
        ctx.log.info(s"Received message $msg")

        val scanner = new Scanner(msg.file)
        var excludedWords:List[String] = List()

        while (scanner.hasNextLine) {
          excludedWords = excludedWords appended scanner.nextLine()
        }

        val gatherer = ctx.spawn(Gatherer(msg.limit, controller), "gatherer")
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