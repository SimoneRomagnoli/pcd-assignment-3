package part2.akka

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.receptionist.{Receptionist, ServiceKey}
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import part2.akka.PuzzleBehaviors.Joiner.Joinable
import part2.akka.PuzzleBehaviors.Messages.Event
import part2.akka.PuzzleBehaviors.Player.{Command, PlayerServiceKey, SelectedRemoteCell}
import part2.akka.PuzzleBehaviors.SelectionGuardian.Selection
import part2.akka.Tiles.Tile

object PuzzleBehaviors {

  object Messages {
    sealed trait Event
  }

  object Starter {
    def apply(): Behavior[Event] = Behaviors.setup[Event] { ctx =>
      val puzzleOptions:PuzzleOptions = PuzzleOptions(3, 5, starter = true)

      val child = ctx.spawn(Player(1, puzzleOptions), s"player1")
      ctx.log.info(s"Registering first player (${child.path}) with receptionist")
      ctx.system.receptionist ! Receptionist.Register(PlayerServiceKey, child)

      Behaviors.empty
    }
  }

  object Joiner {
    val JoinerServiceKey: ServiceKey[Event] = ServiceKey[Event]("joiner")

    final case class Joinable(id:Int, rows:Int, currentPositions:List[Int], selectionList:List[Int]) extends Event with CborSerializable

    def apply(): Behavior[Event] = Behaviors.setup[Event] { ctx =>
      ctx.system.receptionist ! Receptionist.Register(JoinerServiceKey, ctx.self)

      Behaviors.receiveMessage {
        case Joinable(id, rows, currentPositions, selectionList) =>
          ctx.system.receptionist ! Receptionist.Deregister(JoinerServiceKey, ctx.self)
          ctx.log.info(s"Deregistering myself (${ctx.self.path}) with receptionist")
          val puzzleOptions:PuzzleOptions = PuzzleOptions(rows, currentPositions.size / rows, starter = false, currentPositions = currentPositions, selectionList = selectionList)
          val child = ctx.spawn(Player(id, puzzleOptions), s"player$id")
          ctx.log.info(s"Registering a new player (${child.path}) with receptionist")
          ctx.system.receptionist ! Receptionist.Register(PlayerServiceKey, child)
          Behaviors.empty
      }
    }
  }

  object Player {
    val PlayerServiceKey: ServiceKey[Command] = ServiceKey[Command]("player")

    sealed trait Command
    private final case class JoinersUpdated(newJoiners: Set[ActorRef[Event]]) extends Command with CborSerializable
    final case class SelectedRemoteCell(id:Int, selectedCurrentPosition:Int) extends Command with CborSerializable

    def apply(id:Int, puzzleOptions: PuzzleOptions): Behavior[Command] = Behaviors.setup { ctx =>
      val selectionGuardian = ctx.spawn(SelectionGuardian(id, puzzleOptions.selectionList), "selectionGuardian")
      val puzzleBoard:PuzzleBoard =
        PuzzleBoard(id, puzzleOptions.rows, puzzleOptions.cols, puzzleOptions.starter,
          puzzleOptions.currentPositions, puzzleOptions.selectionList, selectionGuardian)

      val subscriptionAdapter = ctx.messageAdapter[Receptionist.Listing]{
        case Joiner.JoinerServiceKey.Listing(joiners) =>
          JoinersUpdated(joiners)
      }
      ctx.system.receptionist ! Receptionist.Subscribe(Joiner.JoinerServiceKey, subscriptionAdapter)
      running(ctx, id, IndexedSeq.empty, puzzleBoard, selectionGuardian)
    }

    private def running(ctx:ActorContext[Command], id:Int, joiners:IndexedSeq[ActorRef[Event]], puzzleBoard: PuzzleBoard, selectionGuardian: ActorRef[Selection]): Behavior[Command] = {
      Behaviors.receiveMessage {
        case JoinersUpdated(newJoiners) =>
          for(joiner <- newJoiners) {
            if(!joiners.map(j => j.path).contains(joiner.path)) {
              val currentPositions:List[Int] = puzzleBoard.tiles.sorted((a:Tile, b:Tile) => a.originalPosition-b.originalPosition).map(tile => tile.currentPosition)
              joiner ! Joinable(newJoiners.size+1, puzzleBoard.rows, currentPositions, puzzleBoard.selectionList)
            }
          }
          running(ctx, id, newJoiners.toIndexedSeq, puzzleBoard, selectionGuardian)
        case SelectedRemoteCell(remoteId, selectedCurrentPosition) =>
          if(remoteId != id) {
            ctx.log.info(s"actor number $remoteId clicked on cell $selectedCurrentPosition")
            puzzleBoard.remoteSelection(selectedCurrentPosition, remoteId)
          } else {
            ctx.log.info(s"actor number $id (me) clicked on cell $selectedCurrentPosition")
            puzzleBoard.localSelection(selectedCurrentPosition, id)
          }
          running(ctx, id, joiners, puzzleBoard, selectionGuardian)
      }
    }
  }

  object SelectionGuardian {
    sealed trait Selection
    private final case class PlayersUpdated(newPlayers: Set[ActorRef[Command]]) extends Selection

    final case class SelectedCell(currentPosition:Int) extends Selection with CborSerializable

    trait Listener {
      def swapAction():Unit
    }

    def apply(id:Int, selectionList:List[Int]): Behavior[Selection] = Behaviors.setup { ctx =>
      val subscriptionAdapter = ctx.messageAdapter[Receptionist.Listing]{
        case Player.PlayerServiceKey.Listing(players) =>
          PlayersUpdated(players)
      }
      ctx.system.receptionist ! Receptionist.Subscribe(Player.PlayerServiceKey, subscriptionAdapter)

      running(ctx, id, selectionList, IndexedSeq.empty)
    }

    private def running(ctx:ActorContext[Selection], id:Int, selections:List[Int] = List(), players:IndexedSeq[ActorRef[Command]]): Behavior[Selection] = {
      Behaviors.receiveMessage{
        case SelectedCell(currentPosition) =>
          players.foreach(player => {
            player ! SelectedRemoteCell(id, currentPosition)
          })
          Behaviors.same
        case PlayersUpdated(newPlayers) =>
          running(ctx, id, selections, newPlayers.toIndexedSeq)
      }
    }

  }

}
