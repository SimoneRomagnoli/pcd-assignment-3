package part2.akka.actors

import akka.actor.typed.Behavior
import akka.actor.typed.receptionist.{Receptionist, ServiceKey}
import akka.actor.typed.scaladsl.Behaviors
import part2.akka.board.PuzzleOptions

object StartBehaviors {

  sealed trait Event

  object Starter {
    def apply(): Behavior[Event] = Behaviors.setup[Event] { ctx =>
      val puzzleOptions: PuzzleOptions =
        PuzzleOptions(starter = true)

      ctx.spawn(Player(1, puzzleOptions), s"player1")
      Behaviors.empty
    }
  }

  object Joiner {
    val JoinerServiceKey: ServiceKey[Event] = ServiceKey[Event]("joiner")
    final case class Joinable(id: Int, currentPositions: List[Int], selectionList: List[Int]) extends Event with CborSerializable

    def apply(): Behavior[Event] = Behaviors.setup[Event] { ctx =>
      ctx.system.receptionist ! Receptionist.Register(JoinerServiceKey, ctx.self)

      Behaviors.receiveMessage {
        case Joinable(id, currentPositions, selectionList) =>
          ctx.system.receptionist ! Receptionist.Deregister(JoinerServiceKey, ctx.self)

          val puzzleOptions: PuzzleOptions =
            PuzzleOptions(starter = false, currentPositions = currentPositions, selectionList = selectionList)

          ctx.spawn(Player(id, puzzleOptions), s"player$id")
          Behaviors.empty
      }
    }
  }

}
