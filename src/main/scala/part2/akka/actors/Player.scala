package part2.akka.actors

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import part2.akka.actors.Guardian.{GuardianMessage, LocalStatus, SelectionRequest}
import part2.akka.board.Puzzle.{PuzzleBoard, PuzzleOptions}
import part2.akka.board.Tiles.Tile

import scala.collection.mutable

/**
 * Behavior of a player in the actor system.
 * It interacts with the gui and with the local guardian.
 *
 */
object Player {

  sealed trait PlayerMessage

  final case class SelectedCell(currentPosition: Int, timestamp:Double) extends PlayerMessage with CborSerializable
  final case class SelectedRemoteCell(id: Int, selectedCurrentPosition: Int) extends PlayerMessage with CborSerializable

  final case class RemovePlayer(id: Int) extends PlayerMessage with CborSerializable

  final case class CutStarted() extends PlayerMessage with CborSerializable
  final case class CutFinished() extends PlayerMessage with CborSerializable

  var id = 0

  def apply(id: Int, puzzleOptions: PuzzleOptions): Behavior[PlayerMessage] = Behaviors.setup { ctx =>
    this.id = id

    val guardian =
      ctx.spawn(Guardian(id, ctx.self), s"guardian$id")

    val puzzleBoard: PuzzleBoard =
      PuzzleBoard(id, puzzleOptions.rows, puzzleOptions.cols, puzzleOptions.starter,
        puzzleOptions.currentPositions, puzzleOptions.selectionList, ctx.self)

    inGame(ctx, puzzleBoard, guardian)
  }

  /**
   * Behavior of the player during the game
   *
   * @param ctx, a reference to the actor's context
   * @param puzzleBoard, the gui
   * @param guardian, a reference to the local guardian
   * @return the player's behavior
   */
  private def inGame(ctx: ActorContext[PlayerMessage],
                     puzzleBoard: PuzzleBoard,
                     guardian: ActorRef[GuardianMessage]
                     ): Behavior[PlayerMessage] = {

    Behaviors.receiveMessage {
      case SelectedCell(currentPosition, timestamp) =>
        guardian ! SelectionRequest(currentPosition, timestamp)
        Behaviors.same

      case SelectedRemoteCell(selectedCurrentPosition, remoteId) =>
        puzzleBoard.remoteSelection(selectedCurrentPosition, remoteId)
        Behaviors.same

      case RemovePlayer(id) =>
        puzzleBoard.unselectAll(id)
        Behaviors.same

      case CutStarted() =>
        val currentPositions:List[Int] = puzzleBoard.tiles.sorted((a:Tile, b:Tile) => a.originalPosition-b.originalPosition).map(tile => tile.currentPosition)
        guardian ! LocalStatus(puzzleBoard.selectionList, currentPositions)
        inCut(ctx, puzzleBoard, guardian)
    }
  }

  /**
   * Behavior of the player during a snapshot procedure.
   *
   * @param ctx, a reference to the actor's context
   * @param puzzleBoard, the gui
   * @param guardian, a reference to the local guardian
   * @return the player's behavior during a system cut
   */
  private def inCut(ctx: ActorContext[PlayerMessage],
                    puzzleBoard: PuzzleBoard,
                    guardian: ActorRef[GuardianMessage]
                    ): Behavior[PlayerMessage] = {

    val messageQueue: mutable.Queue[PlayerMessage] = mutable.Queue.empty

    Behaviors.receiveMessage {
      case CutStarted() =>
        Behaviors.same

      case SelectedCell(currentPosition, timestamp) =>
        messageQueue append SelectedCell(currentPosition, timestamp)
        Behaviors.same

      case RemovePlayer(id) =>
        messageQueue append RemovePlayer(id)
        Behaviors.same

      case CutFinished() =>
        for(message <- messageQueue) {
          ctx.self ! message
        }
        inGame(ctx, puzzleBoard, guardian)
    }
  }

}
