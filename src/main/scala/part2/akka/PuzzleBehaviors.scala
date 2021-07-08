package part2.akka

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.receptionist.{Receptionist, ServiceKey}
import akka.actor.typed.scaladsl.Behaviors
import akka.cluster.typed.{Cluster, Join}
import akka.remote.transport.ActorTransportAdapter.AskTimeout
import part2.akka.PuzzleBehaviors.Messages.Event

import scala.util.{Failure, Success}

object PuzzleBehaviors {

  object Messages {
    sealed trait Event
  }

  object Starter {
    def apply(): Behavior[Event] = Behaviors.setup[Event] { ctx =>

      val imgPath: String = "res/bletchley-park-mansion.jpg"
      val puzzleBoard = PuzzleBoard(3, 5, imgPath)
      val playerId = Cluster(ctx.system).state.members.size

      val cluster = Cluster(ctx.system)
      cluster.manager ! Join(cluster.selfMember.address)

      ctx.spawn(Player(playerId, puzzleBoard), s"player$playerId")
      Behaviors.empty
    }
  }

  object Joiner {
    private case class ListingResponse(listing: Receptionist.Listing) extends Event
    private final case class TransformCompleted(originalText: String, transformedText: String) extends Event
    private final case class JobFailed(why: String, text: String) extends Event

    def apply(): Behavior[Event] = Behaviors.setup[Event] { ctx =>
      val cluster = Cluster(ctx.system)
      val listingResponseAdapter = ctx.messageAdapter[Receptionist.Listing](ListingResponse)
      ctx.system.receptionist ! Receptionist.Find(Player.PlayerServiceKey, listingResponseAdapter)

      Behaviors.receiveMessage {
        case ListingResponse(Player.PlayerServiceKey.Listing(listings)) =>
          ctx.ask(listings.head, Player.TransformText("bla", _)) {
            case Success(transformedText) => TransformCompleted("BLA", transformedText.text)
            case Failure(ex) => JobFailed("Processing timed out", "bla")
          }
          Behaviors.same
      }
      cluster.manager ! Join(cluster.selfMember.address)

      Behaviors.empty
    }
  }

  object Player {
    val PlayerServiceKey: ServiceKey[Command] = ServiceKey[Command]("Player")

    sealed trait Command
    final case class TransformText(text: String, replyTo: ActorRef[TextTransformed]) extends Command with CborSerializable
    final case class TextTransformed(text: String) extends CborSerializable

    //final case class JoinRequest(replyTo:ActorRef[Event]) extends Event with CborSerializable
    //final case class JoinResponse() extends CborSerializable

    def apply(id:Int, puzzleBoard: PuzzleBoard): Behavior[Command] = Behaviors.setup { ctx =>
      ctx.log.info(s"Registering myself (${ctx.self.path}) with receptionist")
      ctx.system.receptionist ! Receptionist.Register(PlayerServiceKey, ctx.self)

      Behaviors.receiveMessage {
        case TransformText(text, replyTo) =>
          ctx.log.info("i received a join request")
          val maxMembers = ctx.system.settings.config.getInt("distributed_puzzle_info.max-members")
          if(Cluster(ctx.system).state.members.size < maxMembers) {

          } else {

          }
          Behaviors.same
      }

    }
  }

}
