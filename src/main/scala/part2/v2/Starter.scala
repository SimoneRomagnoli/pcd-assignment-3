package part2.v2

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import akka.cluster.typed.Cluster

//#frontend
object Starter {

  sealed trait None


  def apply(): Behavior[None] = Behaviors.setup { ctx =>
    val cluster = Cluster(ctx.system);

    val puzzle:PuzzleBoard = PuzzleBoard(3, 5, starter = true)
    ctx.log.info("PUZZLE CREATED")

    ctx.spawn(PlayerBehavior(puzzle), s"player" + (cluster.state.members.size-1))
    Behaviors.stopped
  }
}
