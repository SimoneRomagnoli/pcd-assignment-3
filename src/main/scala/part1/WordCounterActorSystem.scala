package part1

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.text.PDFTextStripper
import part1.Messages.{Directory, Parameters, Pdf, Text, Occurrences}

import java.io.File
import java.nio.file.{Files, Paths}
import java.util.Scanner

object Messages {
  final case class Parameters(directoryPath:String, file:File, limit:Int, updateMethod:String=>Unit)
  final case class Directory(path: String)
  final case class Pdf(document: PDDocument, title:String)
  final case class Text(text: String, title:String)
  final case class Occurrences(occurrences: Map[String, Int], title:String)
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
      val child = ctx.spawn(StripperChild(counter), msg.title)
      child ! msg
      Behaviors.same
  }
}

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
  var globalOccurrences:Map[String, Int] = Map()

  def apply(limit:Int, update:String => Unit): Behavior[Occurrences] = Behaviors.receive { (ctx, msg) =>
    ctx.log.info(s"Gathering words from file ${msg.title}")

    for((k,v) <- msg.occurrences) {
      if(globalOccurrences.contains(k)) {
        globalOccurrences = globalOccurrences + (k -> (globalOccurrences(k)+v))
      } else {
        globalOccurrences = globalOccurrences + (k -> v)
      }
    }

    update(
      globalOccurrences
        .keySet
        .toList
        .sorted((a:String,b:String) => globalOccurrences(b) - globalOccurrences(a))
        .take(limit)
        .map(s => s+" - "+globalOccurrences(s))
        .reduce((a:String, b:String) => a+"\n"+b)
    )

    Behaviors.same
  }
}


case class WordCounter() {
  val system:ActorSystem[Parameters] = ActorSystem[Parameters](Behaviors.setup { ctx =>

    Behaviors.receiveMessage {
      case msg if Files.exists(Paths.get(msg.directoryPath)) && Files.exists(Paths.get(msg.file.getAbsolutePath)) =>
        ctx.log.info(s"Received message $msg")

        val scanner = new Scanner(msg.file)
        var excludedWords:List[String] = List()

        while (scanner.hasNextLine) {
          excludedWords = excludedWords appended scanner.nextLine()
        }

        val gatherer = ctx.spawn(Gatherer(msg.limit, msg.updateMethod), "gatherer")
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