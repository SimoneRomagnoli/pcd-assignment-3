package v2

import akka.actor.{Actor, ActorLogging, Props}
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import akka.cluster.ClusterEvent
import akka.cluster.ClusterEvent.{ClusterDomainEvent, CurrentClusterState, MemberEvent, MemberJoined, MemberUp, UnreachableMember}
import akka.cluster.typed.{Cluster, Subscribe}

object PlayerBehavior {
  sealed trait Event
  final case class Greet(whom: String, replyTo: ActorRef[Greeted]) extends Event
  final case class Greeted(whom: String, from: ActorRef[Greet]) extends Event


  def apply(): Behavior[Event] = Behaviors.setup(ctx => {
    val cluster = Cluster(ctx.system)
    ctx.log.debug("Player Ready" + cluster.selfMember.address)

    //cluster.subscriptions ! Subscribe(ctx.spawn(Subscriber(), "subscriber"), classOf[ClusterEvent.MemberJoined])
    val subscriber = ctx.system.systemActorOf(Subscriber(),"subscriber")

    cluster.subscriptions ! Subscribe(subscriber, classOf[MemberEvent])

    Behaviors.receiveMessage {
      case Greet(whom, replyTo) => ???
      case Greeted(whom, from) => ???
        Behaviors.same
    }
  })

  object Subscriber {
    def apply(): Behavior[MemberEvent] = Behaviors.setup(ctx => {
      ctx.log.debug("SUBSCRIBER READY")

      Behaviors.receiveMessage{
          case MemberJoined(member) => ctx.log.debug("JOIN RECEIVED " + member)
            Behaviors.same
          case _ => ctx.log.debug("DIOPORCO")
            Behaviors.same
        }
      })
  }


}
