package part2.v2

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import akka.cluster.ClusterEvent.{MemberEvent, MemberJoined}
import akka.cluster.typed.{Cluster, Subscribe}
import part2.v2.Joiner.PuzzleReceived

object PlayerBehavior {

  sealed trait Event
  final case class Greet(whom: String, replyTo: ActorRef[Greeted]) extends Event
  final case class Greeted(whom: String, from: ActorRef[Greet]) extends Event


  def apply(puzzle: PuzzleBoard): Behavior[Event] = Behaviors.setup(ctx => {
    val cluster = Cluster(ctx.system)
    ctx.log.info("PLAYER READY" + cluster.selfMember.address)
    puzzle.paintBoard()
    // subscriber che riceverà tutti gli eventi del cluster
    val subscriber = ctx.system.systemActorOf(Subscriber(puzzle), "subscriber")

    cluster.subscriptions ! Subscribe(subscriber, classOf[MemberEvent])

    Behaviors.receiveMessage {
      case Greet(whom, replyTo) => ???
      case Greeted(whom, from) => ???
        Behaviors.same
    }
  })

  object Subscriber {
    def apply(puzzle:PuzzleBoard): Behavior[MemberEvent] = Behaviors.setup(ctx => {
      ctx.log.info("SUBSCRIBER READY")

      Behaviors.receiveMessage {
        case MemberJoined(member) => ctx.log.info("JOIN RECEIVED " + member)
          Behaviors.same
        case _ => ctx.log.info("DIOPORCO")
          Behaviors.same
      }
    })
  }


}
