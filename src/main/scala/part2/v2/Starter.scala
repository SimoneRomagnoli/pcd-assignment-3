package part2.v2

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import akka.cluster.typed.Cluster

//#frontend
object Starter {

  sealed trait None


  def apply(): Behavior[None] = Behaviors.setup { ctx =>
    val cluster = Cluster(ctx.system);

    ctx.log.debug("SETTING TILES")
    //cluster.manager ! Join(cluster.selfMember.address);

    ctx.spawn(PlayerBehavior(), "player1")
    // cluster.subscriptions ! Subscribe(subscriber, classOf[ClusterEvent.MemberJoined])
    Behaviors.stopped
  }
}
