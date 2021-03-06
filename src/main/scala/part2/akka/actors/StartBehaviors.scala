package part2.akka.actors

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.receptionist.{Receptionist, ServiceKey}
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import part2.akka.actors.Guardian.{GuardianMessage, JoinRequest}
import part2.akka.board.Puzzle.PuzzleOptions

/**
 * Contains the possible behaviors of a starting game.
 *
 */
object StartBehaviors {

  sealed trait StartEvent

  /**
   * Starts a game.
   *
   */
  object Starter {
    def apply(): Behavior[StartEvent] = Behaviors.setup[StartEvent] { ctx =>
      val puzzleOptions: PuzzleOptions =
        PuzzleOptions(starter = true)

      ctx.spawn(Player(1, puzzleOptions), s"player1")
      Behaviors.empty
    }
  }

  /**
   * Joins a game.
   *
   */
  object Joiner {

    final case class GuardianReference(guardian: ActorRef[GuardianMessage]) extends StartEvent with CborSerializable
    final case class GlobalStatus(id: Int, currentPositions: List[Int], selectionList: List[Int]) extends StartEvent with CborSerializable

    val JoinerServiceKey: ServiceKey[StartEvent] = ServiceKey[StartEvent]("joiner")

    def apply(): Behavior[StartEvent] = Behaviors.setup[StartEvent] { ctx =>
      ctx.system.receptionist ! Receptionist.Register(JoinerServiceKey, ctx.self)

      Behaviors.receiveMessage {
        case GuardianReference(guardian) =>
          guardian ! JoinRequest(ctx.self)
          inJoin(ctx)
      }
    }

    def inJoin(ctx:ActorContext[StartEvent]): Behavior[StartEvent] = {
      Behaviors.receiveMessage {
        case GuardianReference(_) =>
            Behaviors.same

        case GlobalStatus(id, currentPositions, selectionList) =>
          ctx.system.receptionist ! Receptionist.Deregister(JoinerServiceKey, ctx.self)

          val puzzleOptions: PuzzleOptions =
            PuzzleOptions(starter = false, currentPositions = currentPositions, selectionList = selectionList)

          ctx.spawn(Player(id, puzzleOptions), s"player$id")
          Behaviors.empty
      }
    }
  }

}
