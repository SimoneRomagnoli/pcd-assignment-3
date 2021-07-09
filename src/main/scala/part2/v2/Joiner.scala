package part2.v2

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import akka.cluster.typed.Cluster

//#joiner
object Joiner {

  sealed trait Event
  case class PuzzleReceived(puzzle:PuzzleBoard) extends Event

  def apply(): Behavior[Event] =
    Behaviors.setup { ctx =>
      ctx.log.info("NEW JOINER")
      val cluster = Cluster(ctx.system)
      //cluster.manager ! Join(cluster.selfMember.address)
      //ctx.spawn(PlayerBehavior(), "player")
      Behaviors.stopped
    }
}
