package part2.akka

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.receptionist.{Receptionist, ServiceKey}
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import part2.akka.PuzzleBehaviors.Joiner.Joinable
import part2.akka.PuzzleBehaviors.Messages.Event
import part2.akka.PuzzleBehaviors.Player.PlayerServiceKey
import part2.akka.Tiles.Tile

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

    final case class Joinable(id:Int, rows:Int, currentPositions:List[Int]) extends Event with CborSerializable

    def apply(): Behavior[Event] = Behaviors.setup[Event] { ctx =>
      ctx.system.receptionist ! Receptionist.Register(JoinerServiceKey, ctx.self)

      Behaviors.receiveMessage {
        case Joinable(id, rows, currentPositions) =>
          val puzzleOptions:PuzzleOptions = PuzzleOptions(rows, currentPositions.size / rows, starter = false, currentPositions = currentPositions)
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
      val selectionGuardian = ctx.spawn(SelectionGuardian(), "selectionGuardian")
      val puzzleBoard:PuzzleBoard = PuzzleBoard(puzzleOptions.rows, puzzleOptions.cols, puzzleOptions.starter, puzzleOptions.currentPositions)

      val subscriptionAdapter = ctx.messageAdapter[Receptionist.Listing]{
        case Joiner.JoinerServiceKey.Listing(joiners) =>
          JoinersUpdated(joiners)
      }
      ctx.system.receptionist ! Receptionist.Subscribe(Joiner.JoinerServiceKey, subscriptionAdapter)
      running(ctx, IndexedSeq.empty, puzzleBoard)
    }

    private def running(ctx: ActorContext[Command], joiners:IndexedSeq[ActorRef[Event]], puzzleBoard: PuzzleBoard): Behavior[Command] = {
      Behaviors.receiveMessage {
        case JoinersUpdated(newJoiners) =>
          for(joiner <- newJoiners) {
            if(!joiners.map(j => j.path).contains(joiner.path)) {
              joiner ! Joinable(newJoiners.size, puzzleBoard.rows, puzzleBoard.tiles.sorted((a:Tile, b:Tile) => a.originalPosition-b.originalPosition).map(tile => tile.currentPosition))
            }
          }
          running(ctx, newJoiners.toIndexedSeq, puzzleBoard)
      }
    }
  }

  object SelectionGuardian {
    sealed trait Selection

    trait Listener {
      def swapAction():Unit
    }

    def apply(selectedTile:Tile = null, selectionActive:Boolean = false): Behavior[Selection] = Behaviors.setup { ctx =>

      Behaviors.same
    }

  }

}
