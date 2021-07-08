package v2

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.receptionist.{Receptionist, ServiceKey}
import akka.actor.typed.scaladsl.Behaviors
import akka.cluster.typed.{Cluster, Join}
import v2.PlayerBehavior.{Event, Greet}

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
//#joiner