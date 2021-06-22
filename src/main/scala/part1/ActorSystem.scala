package part1

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.text.PDFTextStripper
import part1.GraphicalUserInterface.ObservableOccurrences
import part1.Messages.{Directory, Pdf, Text}

import java.io.File

object Messages {
  final case class Directory(path: String)
  final case class Pdf(document: PDDocument, title:String)
  final case class Text(text: String, title:String)
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
  def apply(ignoredWords:List[String], occurrences:List[ObservableOccurrences]): Behavior[Text] = Behaviors.receive { (ctx, msg) =>
    ctx.log.info(s"Received raw text of document ${msg.title}")
    val child = ctx.spawn(SplitFilterCountBehavior(ignoredWords, occurrences), msg.title)
    child ! msg
    Behaviors.same
  }
}

object SplitFilterCountBehavior {
  val REGEX = "[^a-zA-Z0-9]"

  def apply(ignoredWords:List[String], globalOccurrences:List[ObservableOccurrences]): Behavior[Text] = Behaviors.receive { (ctx, msg) =>
    ctx.log.info(s"Start processing raw text of document ${msg.title}")
    var occurrences: List[Occurrences] = List()
    val filteredWords = msg.text.split(REGEX).filter(word => !ignoredWords.contains(word))

    for(word <- filteredWords) {
      if(occurrences.exists(oc => oc.word == word)) {
        occurrences.filter(oc => oc.word == word).head.increment()
      } else {
        occurrences = Occurrences(word, 1) :: occurrences
      }
    }
    ctx.log.info("entire list is "+occurrences)
    ctx.log.info("top list words are: "+occurrences.sorted((a:Occurrences,b:Occurrences) => b.occurrences-a.occurrences).slice(0,5).toString())
    Behaviors.stopped
  }
}

case class Occurrences(word:String, var occurrences:Int) {
  def increment(): Unit = occurrences += 1
}

case class WordCounter(occurrences: List[ObservableOccurrences]) {
  val system:ActorSystem[String] = ActorSystem[String](Behaviors.setup { ctx =>

    val ignoredWords = List("document", "PDF", "", " ")

    val counter = ctx.spawn(Counter(ignoredWords, occurrences), "counter")
    val stripper = ctx.spawn(Stripper(counter), "stripper")
    val extractor = ctx.spawn(Extractor(stripper), "extractor")

    Behaviors.receiveMessage {
      case "" =>
        ctx.log.info("Received empty directory")
        Behaviors.stopped
      case msg =>
        ctx.log.info(s"Received message $msg")
        extractor ! Directory(msg)
        Behaviors.same
    }
  }, name = "actor-based-word-counter")

  def !(msg:String): Unit = system ! msg
}