package part2.akka

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.receptionist.{Receptionist, ServiceKey}
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.cluster.typed.Cluster
import part2.akka.PuzzleBehaviors.Joiner.Joinable
import part2.akka.PuzzleBehaviors.Messages.Event
import part2.akka.PuzzleBehaviors.Player.PlayerServiceKey
import part2.akka.Tiles.Tile

object PuzzleBehaviors {

  object Messages {
    sealed trait Event
  }

  object Starter {
    def apply(): Behavior[Event] = Behaviors.setup[Event] { ctx =>
      val imgPath: String = "res/bletchley-park-mansion.jpg"
      val puzzleBoard = PuzzleBoard(3, 5, imgPath, starter = true)
      val playerId = Cluster(ctx.system).state.members.size

      ctx.log.info(s"tiles is ${puzzleBoard.tiles}")

      val child = ctx.spawn(Player(playerId, puzzleBoard), s"player$playerId")
      ctx.log.info(s"Registering myself (${ctx.self.path}) with receptionist")
      ctx.system.receptionist ! Receptionist.Register(PlayerServiceKey, child)

      Behaviors.empty
    }
  }

  object Joiner {
    val JoinerServiceKey: ServiceKey[Event] = ServiceKey[Event]("joiner")

    final case class Joinable(id:Int, rows:Int, imgPath:String, currentPositions:List[Int]) extends Event with CborSerializable

    def apply(): Behavior[Event] = Behaviors.setup[Event] { ctx =>
      ctx.system.receptionist ! Receptionist.Register(JoinerServiceKey, ctx.self)

      Behaviors.receiveMessage {
        case Joinable(id, rows, imgPath, currentPositions) =>
          ctx.log.info(s"------- I AM GOING TO START A NEW BOARD ------------")
          ctx.log.info(s"current positions are $currentPositions")
          val puzzleBoard = PuzzleBoard(rows, currentPositions.size / rows, imgPath, starter = false, currentPositions = currentPositions)
          val child = ctx.spawn(Player(id, puzzleBoard), s"player$id")
          ctx.log.info(s"Registering myself-joiner (${ctx.self.path}) with receptionist")
          ctx.system.receptionist ! Receptionist.Register(PlayerServiceKey, child)
          Behaviors.empty
      }
    }
  }

  object Player {
    val PlayerServiceKey: ServiceKey[Command] = ServiceKey[Command]("player")

    sealed trait Command
    private final case class JoinersUpdated(newJoiners: Set[ActorRef[Event]]) extends Command

    def apply(id:Int, puzzleBoard: PuzzleBoard): Behavior[Command] = Behaviors.setup { ctx =>
      val subscriptionAdapter = ctx.messageAdapter[Receptionist.Listing]{
        case Joiner.JoinerServiceKey.Listing(joiners) =>
          JoinersUpdated(joiners)
      }
      ctx.system.receptionist ! Receptionist.Subscribe(Joiner.JoinerServiceKey, subscriptionAdapter)
      running(ctx, IndexedSeq.empty, puzzleBoard)
    }

    private def running(ctx: ActorContext[Command], joiners:IndexedSeq[ActorRef[Event]], puzzleBoard: PuzzleBoard): Behavior[Command] = {
      Behaviors.receiveMessage {
        case JoinersUpdated(newJoiners) =>
          for(joiner <- newJoiners) {
            if(!joiners.map(j => j.path).contains(joiner.path)) {
              ctx.log.info(s"tiles list is ${puzzleBoard.tiles}")
              ctx.log.info(s"but if i order it, then it becomes ${puzzleBoard.tiles.sorted((a:Tile, b:Tile) => a.originalPosition-b.originalPosition)}")
              joiner ! Joinable(newJoiners.size, puzzleBoard.rows, "res/bletchley-park-mansion.jpg", puzzleBoard.tiles.sorted((a:Tile, b:Tile) => a.originalPosition-b.originalPosition).map(tile => tile.currentPosition))
            }
          }
          running(ctx, newJoiners.toIndexedSeq, puzzleBoard)
      }
    }
  }

  /*
  object Guardian {
    def apply(): Behavior[Receptionist.Listing] = {
      Behaviors.setup[Receptionist.Listing] { ctx =>
        ctx.system.receptionist ! Receptionist.Find(Player.PlayerServiceKey, ctx.self)

        Behaviors.receiveMessagePartial[Receptionist.Listing] {
          case Player.PlayerServiceKey.Listing(listings) =>
            ctx.log.info(s"guardian says listings are $listings")
            //listings.foreach(ps => context.spawnAnonymous(Pinger(ps)))
            Behaviors.same
          case _ => Behaviors.same
        }
      }
    }
  }

   */

}
