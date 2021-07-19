package part2.akka

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorSystem, Behavior}
import akka.cluster.typed.Cluster
import com.typesafe.config.ConfigFactory
import part2.akka.actors.StartBehaviors.{Joiner, Starter}

/**
* Launch an actor system that joins a puzzle game.
*
*/
object JoinerActor1 {

  def main(args:Array[String]): Unit = {
    if(args.isEmpty) {
      startup(0)
    } else {
      require(args.length == 1, "Usage: port")
      startup(args(0).toInt)
    }
  }

  /**
   * Override the configuration of the port and role
   * and create the actor system.
   *
   * @param port the starting port of the actor system (if 0 it chooses a random port)
   */
  def startup(port:Int): Unit = {
    val role:String = if(port != 0) "starter" else "joiner"
    val config = ConfigFactory
      .parseString(s"""
      akka.remote.artery.canonical.port=$port
      akka.cluster.roles = [$role]
      """)
      .withFallback(ConfigFactory.load("distributed_puzzle"))
    ActorSystem[Nothing](RootBehavior(), "DistributedPuzzle", config)
  }

  /**
   * Starts a root behavior depending on the role of the node
   * being Starter or Joiner.
   *
   */
  object RootBehavior {
    def apply(): Behavior[Nothing] = Behaviors.setup[Nothing] { ctx =>
      val cluster = Cluster(ctx.system)
      if(cluster.selfMember.hasRole("starter")) {
        ctx.log.info("i spawn a starter")
        ctx.spawn(Starter(), "Starter")
      } else {
        ctx.log.info("i spawn a joiner")
        ctx.spawn(Joiner(), s"Joiner${cluster.state.members.size-1}")
      }
      Behaviors.empty
    }
  }
}
