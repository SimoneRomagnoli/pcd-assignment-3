package part2.akka

import akka.actor.Status.{Failure, Success}
import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.receptionist.{Receptionist, ServiceKey}
import akka.actor.typed.scaladsl.AskPattern.Askable
import akka.actor.typed.scaladsl.Behaviors
import akka.cluster.typed.{Cluster, Join}
import part2.akka.PuzzleBehaviors.Messages.{Event, JoinRequest, JoinResponse}

object PuzzleBehaviors {

  object Messages {
    sealed trait Event
    final case class JoinRequest(replyTo:ActorRef[Event]) extends Event with CborSerializable
    final case class JoinResponse() extends CborSerializable
  }

  object Starter {
    def apply(): Behavior[Event] = Behaviors.setup[Event] { ctx =>

      val imgPath: String = "res/bletchley-park-mansion.jpg"
      val puzzleBoard = PuzzleBoard(3, 5, imgPath)
      val playerId = Cluster(ctx.system).state.members.size

      val cluster = Cluster(ctx.system)
      cluster.manager ! Join(cluster.selfMember.address)

      ctx.spawn(Player(playerId, puzzleBoard), s"player$playerId")
      Behaviors.empty
    }
  }

  object Joiner {
    def apply(): Behavior[Event] = Behaviors.setup[Event] { ctx =>
      val cluster = Cluster(ctx.system)

      /*
      ctx.ask(ctx.system.receptionist.narrow, JoinRequest) {
        case Success(_) => ???
        case Failure(_) => ???
      }

       */

      cluster.manager ? JoinRequest(ctx.self)

      cluster.manager ! Join(cluster.selfMember.address)

      Behaviors.empty
    }
  }

  object Player {
    val playerServiceKey: ServiceKey[Event] = ServiceKey[Event]("Player")

    def apply(id:Int, puzzleBoard: PuzzleBoard): Behavior[Event] = Behaviors.setup { ctx =>
      ctx.log.info(s"Registering myself (${ctx.self.path}) with receptionist")
      ctx.system.receptionist ! Receptionist.Register(playerServiceKey, ctx.self)

      Behaviors.receiveMessage {
        case JoinRequest(replyTo) =>
          ctx.log.info("i received a join request")
          val maxMembers = ctx.system.settings.config.getInt("distributed_puzzle_info.max-members")
          if(Cluster(ctx.system).state.members.size < maxMembers) {

          } else {

          }
          Behaviors.same
      }

    }
  }

}
