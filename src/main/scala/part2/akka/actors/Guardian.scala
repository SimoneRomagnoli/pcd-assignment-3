package part2.akka.actors

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.receptionist.{Receptionist, ServiceKey}
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import part2.akka.actors.Player.{CutFinished, CutStarted, PlayerMessage, SelectedRemoteCell}
import part2.akka.actors.StartBehaviors.Joiner.Joinable
import part2.akka.actors.StartBehaviors.{Event, Joiner}

import scala.collection.mutable

object Guardian {
  val GuardianServiceKey: ServiceKey[GuardianMessage] = ServiceKey[GuardianMessage]("guardian")

  sealed trait GuardianMessage

  //SELECTION MESSAGES
  final case class SelectionRequest(currentPosition:Int) extends GuardianMessage with CborSerializable
  final case class RemoteSelection(currentPosition: Int, id:Int) extends GuardianMessage with CborSerializable

  //CUT MESSAGES
  final case class Marker(replyTo:ActorRef[GuardianMessage], joiner:ActorRef[Event]) extends GuardianMessage with CborSerializable
  final case class LocalStatus(selectionList:List[Int], currentPositions:List[Int]) extends GuardianMessage with CborSerializable
  final case class RemoteStatus(selectionList:List[Int], currentPositions:List[Int]) extends GuardianMessage with CborSerializable
  final case class StopCut() extends GuardianMessage with CborSerializable

  //RECEPTIONIST UPDATES
  private final case class GuardiansUpdated(newGuardians: Set[ActorRef[GuardianMessage]]) extends GuardianMessage
  private final case class JoinersUpdated(newJoiners: Set[ActorRef[Event]]) extends GuardianMessage

  var id = 0

  def apply(id: Int, player:ActorRef[PlayerMessage]): Behavior[GuardianMessage] = Behaviors.setup { ctx =>
    this.id = id

    val receptionistSubscriptionAdapter = ctx.messageAdapter[Receptionist.Listing] {
      case Joiner.JoinerServiceKey.Listing(joiners) =>
        JoinersUpdated(joiners)
      case GuardianServiceKey.Listing(guardians) =>
        GuardiansUpdated(guardians)

    }

    ctx.system.receptionist ! Receptionist.Subscribe(Guardian.GuardianServiceKey, receptionistSubscriptionAdapter)
    ctx.system.receptionist ! Receptionist.Subscribe(Joiner.JoinerServiceKey, receptionistSubscriptionAdapter)

    ctx.system.receptionist ! Receptionist.Register(GuardianServiceKey, ctx.self)

    inGame(ctx, player, IndexedSeq.empty)
  }

  private def inGame(ctx: ActorContext[GuardianMessage],
                     player: ActorRef[PlayerMessage],
                     guardians: IndexedSeq[ActorRef[GuardianMessage]]): Behavior[GuardianMessage] = {

    Behaviors.receiveMessage {
      case GuardiansUpdated(newGuardians) =>
        inGame(ctx, player, newGuardians.toIndexedSeq)

      case JoinersUpdated(joiners) =>
        for (joiner <- joiners; guardian <- guardians) {
          guardian ! Marker(ctx.self, joiner)
        }
        inGame(ctx, player, guardians)

      case SelectionRequest(currentPosition) =>
        for (guardian <- guardians) {
          guardian ! RemoteSelection(currentPosition, this.id)
        }
        inGame(ctx, player, guardians)

      case RemoteSelection(currentPosition, id) =>
        player ! SelectedRemoteCell(currentPosition, id)
        inGame(ctx, player, guardians)

      case Marker(replyTo, joiner) =>
        inCut(ctx, player, guardians, replyTo, joiner)
    }
  }

  private def inCut(ctx: ActorContext[GuardianMessage],
                    player: ActorRef[PlayerMessage],
                    guardians: IndexedSeq[ActorRef[GuardianMessage]],
                    replyTo: ActorRef[GuardianMessage],
                    joiner: ActorRef[Event],
                    selections:mutable.Queue[List[Int]] = mutable.Queue.empty,
                    positions:mutable.Queue[List[Int]] = mutable.Queue.empty
                   ): Behavior[GuardianMessage] = {

    player ! CutStarted()
    Behaviors.receiveMessage {
      case LocalStatus(selectionList, currentPositions) =>
        replyTo ! RemoteStatus(selectionList, currentPositions)
        inCut(ctx, player, guardians, replyTo, joiner, selections, positions)

      case RemoteStatus(selectionList, currentPositions) =>
        selections append selectionList
        positions append currentPositions
        if(guardians.size == selections.size) {
          //find consistent cut
          joiner ! Joinable(guardians.size+1, currentPositions, selectionList)
          for(guardian <- guardians) {
            guardian ! StopCut()
          }

          inCut(ctx, player, guardians, replyTo, joiner, selections, positions)
        } else {
          inCut(ctx, player, guardians, replyTo, joiner, selections, positions)
        }

      case StopCut() =>
        player ! CutFinished()
        inGame(ctx, player, guardians)

      case GuardiansUpdated(newGuardians) =>
        inCut(ctx, player, newGuardians.toIndexedSeq, replyTo, joiner, selections, positions)
    }
  }

}
