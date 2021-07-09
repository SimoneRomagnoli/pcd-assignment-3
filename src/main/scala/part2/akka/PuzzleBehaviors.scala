package part2.akka

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.receptionist.{Receptionist, ServiceKey}
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.remote.transport.ActorTransportAdapter.AskTimeout
import part2.akka.PuzzleBehaviors.Joiner.Joinable
import part2.akka.PuzzleBehaviors.Messages.Event
import part2.akka.PuzzleBehaviors.Player.PlayerServiceKey
import part2.akka.PuzzleBehaviors.SelectionGuardian.Selection
import part2.akka.Tiles.Tile

import scala.util.{Failure, Success}

object PuzzleBehaviors {

  object Messages {
    sealed trait Event
  }

  object Starter {
    def apply(): Behavior[Event] = Behaviors.setup[Event] { ctx =>
      val puzzleOptions:PuzzleOptions = PuzzleOptions(3, 5, starter = true)

      val child = ctx.spawn(Player(1, puzzleOptions), s"player1")
      ctx.log.info(s"Registering myself (${ctx.self.path}) with receptionist")
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
          val puzzleOptions:PuzzleOptions = PuzzleOptions(rows, currentPositions.size / rows, starter = false, currentPositions = currentPositions, selectionList = selectionList)
          val child = ctx.spawn(Player(id, puzzleOptions), s"player$id")
          ctx.log.info(s"Registering myself-joiner (${ctx.self.path}) with receptionist")
          ctx.system.receptionist ! Receptionist.Register(PlayerServiceKey, child)
          Behaviors.empty
      }
    }
  }

  object Player {
    val PlayerServiceKey: ServiceKey[Command] = ServiceKey[Command]("player")

    sealed trait Command
    private final case class JoinersUpdated(newJoiners: Set[ActorRef[Event]]) extends Command

    def apply(id:Int, puzzleOptions: PuzzleOptions): Behavior[Command] = Behaviors.setup { ctx =>
      val selectionGuardian = ctx.spawn(SelectionGuardian(puzzleOptions.selectionList), "selectionGuardian")
      val puzzleBoard:PuzzleBoard = PuzzleBoard(puzzleOptions.rows, puzzleOptions.cols, puzzleOptions.starter, puzzleOptions.currentPositions, puzzleOptions.selectionList)

      val subscriptionAdapter = ctx.messageAdapter[Receptionist.Listing]{
        case Joiner.JoinerServiceKey.Listing(joiners) =>
          JoinersUpdated(joiners)
      }
      ctx.system.receptionist ! Receptionist.Subscribe(Joiner.JoinerServiceKey, subscriptionAdapter)
      running(ctx, IndexedSeq.empty, puzzleBoard, selectionGuardian)
    }

    private def running(ctx:ActorContext[Command], joiners:IndexedSeq[ActorRef[Event]], puzzleBoard: PuzzleBoard, selectionGuardian: ActorRef[Selection]): Behavior[Command] = {
      Behaviors.receiveMessage {
        case JoinersUpdated(newJoiners) =>
          for(joiner <- newJoiners) {
            if(!joiners.map(j => j.path).contains(joiner.path)) {
              joiner ! Joinable(newJoiners.size+1, puzzleBoard.rows, puzzleBoard.currentPositions, puzzleBoard.selectionList)
            }
          }
          running(ctx, newJoiners.toIndexedSeq, puzzleBoard, selectionGuardian)
      }
    }
  }

  object SelectionGuardian {
    sealed trait Selection

    final case class SelectedCell(index:Int) extends Selection with CborSerializable

    trait Listener {
      def swapAction():Unit
    }

    def apply(selectionList:List[Int]): Behavior[Selection] = Behaviors.setup { ctx =>
      running(selectionList)
    }

    private def running(selections:List[Int] = List()): Behavior[Selection] = {
      Behaviors.receiveMessage{
        case SelectedCell(index) =>
        Behaviors.same
      }
    }

  }

}
