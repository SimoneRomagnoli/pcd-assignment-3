package part2.akka.actors

import akka.actor.typed.receptionist.{Receptionist, ServiceKey}
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior}
import part2.akka.actors.PuzzleBehaviors.Joiner.Joinable
import part2.akka.actors.PuzzleBehaviors.Messages.Event
import part2.akka.actors.PuzzleBehaviors.Player.{Command, CutStatus, PlayerServiceKey, SelectedRemoteCell}
import part2.akka.actors.PuzzleBehaviors.SelectionGuardian.{CutRequest, GuardianServiceKey, Selection}
import part2.akka.board.Tiles.Tile
import part2.akka.board.{PuzzleBoard, PuzzleOptions}

import scala.collection.mutable

object PuzzleBehaviors {

  object Messages {
    sealed trait Event
  }

  object Starter {
    def apply(): Behavior[Event] = Behaviors.setup[Event] { ctx =>
      val puzzleOptions: PuzzleOptions =
        PuzzleOptions(3, 5, starter = true)

      val child =
        ctx.spawn(Player(1, puzzleOptions), s"player1")

      ctx.system.receptionist ! Receptionist.Register(PlayerServiceKey, child)

      Behaviors.empty
    }
  }

  object Joiner {
    val JoinerServiceKey: ServiceKey[Event] = ServiceKey[Event]("joiner")
    final case class Joinable(id: Int, rows: Int, currentPositions: List[Int], selectionList: List[Int]) extends Event with CborSerializable

    def apply(): Behavior[Event] = Behaviors.setup[Event] { ctx =>
      ctx.system.receptionist ! Receptionist.Register(JoinerServiceKey, ctx.self)

      Behaviors.receiveMessage {
        case Joinable(id, rows, currentPositions, selectionList) =>
          ctx.system.receptionist ! Receptionist.Deregister(JoinerServiceKey, ctx.self)

          val puzzleOptions: PuzzleOptions =
            PuzzleOptions(rows, currentPositions.size / rows, starter = false, currentPositions = currentPositions, selectionList = selectionList)

          val child =
            ctx.spawn(Player(id, puzzleOptions), s"player$id")

          ctx.system.receptionist ! Receptionist.Register(PlayerServiceKey, child)
          Behaviors.empty
      }
    }
  }

  object Player {
    val PlayerServiceKey: ServiceKey[Command] = ServiceKey[Command]("player")
    sealed trait Command
    private final case class JoinersUpdated(newJoiners: Set[ActorRef[Event]]) extends Command with CborSerializable
    final case class SelectedRemoteCell(id: Int, selectedCurrentPosition: Int) extends Command with CborSerializable
    final case class CutStatus(selections:List[Int], joiner:ActorRef[Event]) extends Command with CborSerializable

    def apply(id: Int, puzzleOptions: PuzzleOptions): Behavior[Command] = Behaviors.setup { ctx =>
      val selectionGuardian =
        ctx.spawn(SelectionGuardian(id, puzzleOptions.selectionList), "selectionGuardian")

      val puzzleBoard: PuzzleBoard =
        PuzzleBoard(id, puzzleOptions.rows, puzzleOptions.cols, puzzleOptions.starter,
          puzzleOptions.currentPositions, puzzleOptions.selectionList, selectionGuardian)

      val subscriptionAdapter = ctx.messageAdapter[Receptionist.Listing] {
        case Joiner.JoinerServiceKey.Listing(joiners) =>
          JoinersUpdated(joiners)
      }

      ctx.system.receptionist ! Receptionist.Subscribe(Joiner.JoinerServiceKey, subscriptionAdapter)
      running(ctx, id, id, puzzleBoard, selectionGuardian, mutable.Queue.empty)
    }

    private def running(ctx: ActorContext[Command], id: Int, joinedPlayers: Int, puzzleBoard: PuzzleBoard, selectionGuardian: ActorRef[Selection], selectionsFromCut: mutable.Queue[List[Int]]): Behavior[Command] = {
      Behaviors.receiveMessage {
        case JoinersUpdated(joiners) =>
          for (joiner <- joiners) {
            selectionGuardian ! CutRequest(ctx.self, joiner)
          }
          running(ctx, id, joinedPlayers, puzzleBoard, selectionGuardian, selectionsFromCut)

        case SelectedRemoteCell(remoteId, selectedCurrentPosition) =>
          if (remoteId != id) {
            puzzleBoard.remoteSelection(selectedCurrentPosition, remoteId)
          } else {
            puzzleBoard.localSelection(selectedCurrentPosition, id)
          }
          running(ctx, id, joinedPlayers, puzzleBoard, selectionGuardian, selectionsFromCut)

        case CutStatus(selections, joiner) =>
          selectionsFromCut append selections
          if(selectionsFromCut.size equals joinedPlayers) {
            ctx.log.info(s"selections from cut is $selectionsFromCut")
            ctx.log.info(s"my selection is ${puzzleBoard.selectionList}")
            if(selectionsFromCut.forall(selectionList => selectionList equals puzzleBoard.selectionList)) {
              ctx.log.info("CONSISTENT CUT")
              val currentPositions: List[Int] = puzzleBoard.tiles.sorted((a: Tile, b: Tile) => a.originalPosition - b.originalPosition).map(tile => tile.currentPosition)
              joiner ! Joinable(joinedPlayers + 1, puzzleBoard.rows, currentPositions, puzzleBoard.selectionList)
              running(ctx, id, joinedPlayers+1, puzzleBoard, selectionGuardian, selectionsFromCut)
            } else {
              ctx.log.info("INCONSISTENT CUT")
            }
          }
          running(ctx, id, joinedPlayers, puzzleBoard, selectionGuardian, selectionsFromCut)
      }
    }
  }

  object SelectionGuardian {
    val GuardianServiceKey: ServiceKey[Selection] = ServiceKey[Selection]("guardian")

    sealed trait Selection

    final case class SelectedCell(currentPosition: Int) extends Selection with CborSerializable
    final case class CutRequest(replyTo:ActorRef[Command], joiner:ActorRef[Event]) extends Selection with CborSerializable
    final case class Marker(replyTo:ActorRef[Command], joiner:ActorRef[Event]) extends Selection with CborSerializable

    private final case class PlayersUpdated(newPlayers: Set[ActorRef[Command]]) extends Selection
    private final case class GuardiansUpdated(newGuardians: Set[ActorRef[Selection]]) extends Selection

    var cutRunning:Boolean = false

    trait Listener {
      def swapAction(): Unit
    }

    def apply(id: Int, selectionList: List[Int]): Behavior[Selection] = Behaviors.setup { ctx =>
      val receptionistSubscriptionAdapter = ctx.messageAdapter[Receptionist.Listing] {
        case GuardianServiceKey.Listing(guardians) =>
          GuardiansUpdated(guardians)
        case Player.PlayerServiceKey.Listing(players) =>
          PlayersUpdated(players)
      }

      ctx.system.receptionist ! Receptionist.Subscribe(SelectionGuardian.GuardianServiceKey, receptionistSubscriptionAdapter)
      ctx.system.receptionist ! Receptionist.Subscribe(Player.PlayerServiceKey, receptionistSubscriptionAdapter)

      ctx.system.receptionist ! Receptionist.Register(GuardianServiceKey, ctx.self)

      running(ctx, id, selectionList, mutable.Queue.empty, IndexedSeq.empty, IndexedSeq.empty)
    }

    private def running(ctx: ActorContext[Selection],
                        id: Int,
                        selections: List[Int] = List(),
                        cutQueue: mutable.Queue[Command],
                        players: IndexedSeq[ActorRef[Command]],
                        guardians: IndexedSeq[ActorRef[Selection]]): Behavior[Selection] = {

      Behaviors.receiveMessage {
        case SelectedCell(currentPosition) =>
          if(cutRunning) {
            cutQueue append SelectedRemoteCell(id, currentPosition)
          } else {
            players.foreach(player => {
              player ! SelectedRemoteCell(id, currentPosition)
            })
          }
          running(ctx, id, selections, cutQueue, players, guardians)

        case CutRequest(replyTo, joiner) =>
          if(!cutRunning) {
            cutRunning = true
            for(guardian <- guardians) {
              guardian ! Marker(replyTo, joiner)
            }
          }
          running(ctx, id, selections, cutQueue, players, guardians)

        case Marker(replyTo, joiner) =>
          cutRunning = true
          replyTo ! CutStatus(selections, joiner)
          running(ctx, id, selections, cutQueue, players, guardians)

        case PlayersUpdated(newPlayers) =>
          if(cutRunning) {
            cutRunning = false
            for(msg <- cutQueue; player <- players) {
              player ! msg
            }
          }
          running(ctx, id, selections, cutQueue, newPlayers.toIndexedSeq, guardians)

        case GuardiansUpdated(newGuardians) =>
          running(ctx, id, selections, cutQueue, players, newGuardians.toIndexedSeq)
      }
    }

  }

}
