package part2.akka.actors

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.receptionist.{Receptionist, ServiceKey}
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import part2.akka.actors.Player.{CutFinished, CutStarted, PlayerMessage, SelectedRemoteCell}
import part2.akka.actors.StartBehaviors.Joiner.Joinable
import part2.akka.actors.StartBehaviors.{Joiner, StartEvent}

import scala.collection.mutable
import scala.math.Ordering.Implicits.seqOrdering

/**
 * Behavior of a guardian:
 * it propagates local changes handling critical sections and receives remote ones;
 * in addition, it takes join requests and responds with a snapshot of the global status of the distributed system.
 *
 */
object Guardian {

  sealed trait GuardianMessage

  //SELECTION MESSAGES
  final case class SelectionRequest(currentPosition:Int, timestamp:Double) extends GuardianMessage with CborSerializable
  final case class RemoteSelection(currentPosition: Int, id:Int) extends GuardianMessage with CborSerializable

  //CRITICAL SECTION MESSAGES
  final case class CriticalSectionRequest(timestamp: Double, replyTo:ActorRef[GuardianMessage]) extends GuardianMessage with CborSerializable
  final case class CriticalSectionOK(remoteId:Int) extends GuardianMessage with CborSerializable

  //CUT MESSAGES
  final case class Marker(replyTo:ActorRef[GuardianMessage], joiner:ActorRef[StartEvent]) extends GuardianMessage with CborSerializable
  final case class LocalStatus(selectionList:List[Int], currentPositions:List[Int]) extends GuardianMessage with CborSerializable
  final case class RemoteStatus(selectionList:List[Int], currentPositions:List[Int], remoteId:Int) extends GuardianMessage with CborSerializable
  final case class StopCut() extends GuardianMessage with CborSerializable

  //RECEPTIONIST UPDATES
  private final case class GuardiansUpdated(newGuardians: Set[ActorRef[GuardianMessage]]) extends GuardianMessage
  private final case class JoinersUpdated(newJoiners: Set[ActorRef[StartEvent]]) extends GuardianMessage

  val GuardianServiceKey: ServiceKey[GuardianMessage] = ServiceKey[GuardianMessage]("guardian")

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

  /**
   * Behavior of the guardian during the game
   *
   * @param ctx, a reference to the actor's context
   * @param player, a reference to the player
   * @param guardians, a list of the other guardians in the game
   * @return the guardian behavior
   */
  private def inGame(ctx: ActorContext[GuardianMessage],
                     player: ActorRef[PlayerMessage],
                     guardians: IndexedSeq[ActorRef[GuardianMessage]],
                     criticalSectionRequests:mutable.Queue[(Double, Int)] = mutable.Queue.empty,
                     criticalSectionReplies:mutable.Queue[Int] = mutable.Queue.empty,
                     remoteCriticalSectionRequests: mutable.Queue[ActorRef[GuardianMessage]] = mutable.Queue.empty
                    ): Behavior[GuardianMessage] = {

    Behaviors.receiveMessage {
      case GuardiansUpdated(newGuardians) =>
        inGame(ctx, player, newGuardians.toIndexedSeq, criticalSectionRequests, criticalSectionReplies, remoteCriticalSectionRequests)

      case JoinersUpdated(joiners) =>
        for (joiner <- joiners; guardian <- guardians) {
          guardian ! Marker(ctx.self, joiner)
        }
        inGame(ctx, player, guardians, criticalSectionRequests, criticalSectionReplies, remoteCriticalSectionRequests)

      case SelectionRequest(currentPosition, timestamp) =>
        criticalSectionRequests.append((timestamp, currentPosition))
        for (guardian <- guardians) {
          guardian ! CriticalSectionRequest(timestamp, ctx.self)
        }
        inGame(ctx, player, guardians, criticalSectionRequests, criticalSectionReplies, remoteCriticalSectionRequests)

      case CriticalSectionRequest(timestamp, replyTo) =>
        if(criticalSectionRequests.isEmpty || criticalSectionRequests.head._1 >= timestamp) {
          replyTo ! CriticalSectionOK(id)
        } else {
          remoteCriticalSectionRequests append replyTo
        }
        inGame(ctx, player, guardians, criticalSectionRequests, criticalSectionReplies, remoteCriticalSectionRequests)

      case CriticalSectionOK(remoteId) =>
        criticalSectionReplies append remoteId
        if(criticalSectionReplies.size == guardians.size) {

          //NOTIFY OTHER GUADIANS OF MY SELECTION IN CRITICAL SECTION
          val currentPosition = criticalSectionRequests.dequeue()._2
          for (guardian <- guardians) {
            guardian ! RemoteSelection(currentPosition, this.id)
          }

          //AWAKE PENDING CRITICAL SECTION REQUESTS
          for(guardian <- remoteCriticalSectionRequests) {
            guardian ! CriticalSectionOK(id)
          }

          //EMPTY PENDING REQUESTS
          remoteCriticalSectionRequests.clear()
          criticalSectionReplies.clear()
        }
        inGame(ctx, player, guardians, criticalSectionRequests, criticalSectionReplies, remoteCriticalSectionRequests)

      case RemoteSelection(currentPosition, id) =>
        player ! SelectedRemoteCell(currentPosition, id)
        inGame(ctx, player, guardians, criticalSectionRequests, criticalSectionReplies, remoteCriticalSectionRequests)

      case Marker(replyTo, joiner) =>
        inCut(ctx, player, guardians, replyTo, joiner)
    }
  }

  /**
   * Behavior of the guardian during the system cut
   *
   * @param ctx, a reference to the actor's context
   * @param player, a reference to the player
   * @param guardians, a list of the other guardians in the game
   * @param replyTo, a reference to the guardian that started the system cut
   * @param joiner, a reference to the actor that wants to join the game
   * @param selections, a queue of the remote selection lists
   * @param positions, a queue of the remote current position lists
   * @return
   */
  private def inCut(ctx: ActorContext[GuardianMessage],
                    player: ActorRef[PlayerMessage],
                    guardians: IndexedSeq[ActorRef[GuardianMessage]],
                    replyTo: ActorRef[GuardianMessage],
                    joiner: ActorRef[StartEvent],
                    selections:mutable.Queue[List[Int]] = mutable.Queue.empty,
                    positions:mutable.Queue[List[Int]] = mutable.Queue.empty,
                    identifiers:mutable.Queue[Int] = mutable.Queue.empty,
                    criticalSectionRequests:mutable.Queue[CriticalSectionRequest] = mutable.Queue.empty,
                   ): Behavior[GuardianMessage] = {

    player ! CutStarted()
    Behaviors.receiveMessage {
      case LocalStatus(selectionList, currentPositions) =>
        replyTo ! RemoteStatus(selectionList, currentPositions, id)
        inCut(ctx, player, guardians, replyTo, joiner, selections, positions, identifiers)

      case RemoteStatus(selectionList, currentPositions, remoteId) =>
        selections append selectionList
        positions append currentPositions
        identifiers append remoteId
        if(guardians.size == selections.size) {
          //SELECT THE MOST RECURRENT STATE AMONG THE LOCAL STATES
          val globalSelectionsCut: List[Int] = selections.toList.groupBy(identity).maxBy { case (_, occurrences) => occurrences.size }._1
          val globalPositionsCut: List[Int] = positions.toList.groupBy(identity).maxBy { case (_, occurrences) => occurrences.size }._1
          val nextId: Int = identifiers.toList.max + 1
          joiner ! Joinable(nextId, globalPositionsCut, globalSelectionsCut)
          for (guardian <- guardians) {
            guardian ! StopCut()
          }
        }
        inCut(ctx, player, guardians, replyTo, joiner, selections, positions, identifiers)

      case StopCut() =>
        player ! CutFinished()
        for(request <- criticalSectionRequests) {
          ctx.self ! request
        }
        inGame(ctx, player, guardians)

      case CriticalSectionRequest(timestamp, replyToCS) =>
        criticalSectionRequests append CriticalSectionRequest(timestamp, replyToCS)
        inCut(ctx, player, guardians, replyTo, joiner, selections, positions, identifiers)

      case GuardiansUpdated(newGuardians) =>
        inCut(ctx, player, newGuardians.toIndexedSeq, replyTo, joiner, selections, positions, identifiers)
    }
  }

}
