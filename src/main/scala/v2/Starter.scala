package v2

import akka.actor.typed._
import akka.actor.typed.scaladsl._
import akka.cluster.ClusterEvent._
import akka.cluster.{ClusterEvent, MemberStatus}
import akka.cluster.typed._

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
//#frontend