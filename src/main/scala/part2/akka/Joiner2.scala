package part2.akka

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorSystem, Behavior}
import akka.cluster.typed.Cluster
import com.typesafe.config.ConfigFactory
import part2.akka.actors.StartBehaviors.{Joiner, Starter}

import java.awt.Color

object Joiner2 {
  val playersToColors: Map[Int, Color] =
    Map(
      (1, Color.green), (2, Color.blue), (3, Color.red),
      (4, Color.orange), (5, Color.magenta), (6, Color.yellow),
      (7, Color.cyan), (8, Color.pink), (9, Color.darkGray)
    )

  object RootBehavior {
    def apply(): Behavior[Nothing] = Behaviors.setup[Nothing] { ctx =>
      val cluster = Cluster(ctx.system)
      ctx.log.info(s"members before init are ${cluster.state.members.size}")
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

  def main(args:Array[String]): Unit = {
    if(args.isEmpty) {
      startup(0)
    } else {
      require(args.length == 1, "Usage: port")
      startup(args(0).toInt)
    }
  }

  def startup(port:Int): Unit = {
    // Override the configuration of the port and role
    val role:String = if(port != 0) "starter" else "joiner"
    val config = ConfigFactory
      .parseString(s"""
      akka.remote.artery.canonical.port=$port
      akka.cluster.roles = [$role]
      """)
      .withFallback(ConfigFactory.load("distributed_puzzle_info"))
    ActorSystem[Nothing](RootBehavior(), "DistributedPuzzle", config)
  }
}
