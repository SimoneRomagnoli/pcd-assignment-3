package part2.v2

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorSystem, Behavior}
import akka.cluster.typed.Cluster
import com.typesafe.config.ConfigFactory

object App {
  object RootBehavior {
    def apply(): Behavior[Nothing] = Behaviors.setup[Nothing] { ctx =>
      val cluster = Cluster(ctx.system)

      // We use roles, defined in config entry akka.cluster.roles, to help the cluster to organise work
      if (cluster.selfMember.hasRole("starter")) {
        ctx.spawn(Starter(), s"Starter")
      }
      if (cluster.selfMember.hasRole("joiner")) {
        ctx.spawn(Joiner(), "Joiner" + (cluster.state.members.size - 1))
      }
      Behaviors.empty
    }


    def main(args: Array[String]): Unit = {
      // starting 2 frontend nodes and 3 backend nodes
      if (args.isEmpty) {
        startup("starter", 2551)
        startup("joiner", 0)
      } else {
        require(args.length == 2, "Usage: role port")
        startup(args(0), args(1).toInt)
      }
    }

    def startup(role: String, port: Int): Unit = {
      // Override the configuration of the port and role
      val config = ConfigFactory
        .parseString(
          s"""
        akka.remote.artery.canonical.port=$port
        akka.cluster.roles = [$role]
        """)
        .withFallback(ConfigFactory.load("application"))

      ActorSystem[Nothing](RootBehavior(), "ClusterSystem", config)

    }
  }
}
