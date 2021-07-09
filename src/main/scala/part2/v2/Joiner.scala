package part2.v2

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import akka.cluster.typed.Cluster

//#joiner
object Joiner {

  //val joinerServiceKey = ServiceKey[Joiner.JoinRequest]("joiner")

  sealed trait Command
  //final case class JoinRequest(text: String, replyTo: ActorRef[TextTransformed]) extends Command with CborSerializable
  //final case class TextTransformed(text: String) extends CborSerializable

  def apply(): Behavior[Command] =
    Behaviors.setup { ctx =>
      ctx.log.info("Joiner wants to join")
      val cluster = Cluster(ctx.system)
      //cluster.manager ! Join(cluster.selfMember.address)
      ctx.spawn(PlayerBehavior(), "player")
      Behaviors.stopped
    }
}
