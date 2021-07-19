package infrastructure.actor

import akka.cluster.sharding.typed.ShardingEnvelope
import scala.concurrent.duration.DurationInt
import akka.cluster.sharding.typed.scaladsl._
import scala.language.postfixOps
import scala.reflect.ClassTag
import akka.util.Timeout
import akka.actor.typed._

abstract class ShardedActor[Command: ClassTag, State]()(
    implicit
    sharding: ClusterSharding,
    system: ActorSystem[Nothing],
    timeout: Timeout = Timeout(20 seconds)
) {

  protected def behavior(id: String)(state: Option[State] = None): Behavior[Command]

  type C = Command
  type M = ShardingEnvelope[C]
  private final def init(
      implicit
      sharding: ClusterSharding,
      system: ActorSystem[Nothing],
      timeout: Timeout = Timeout(20 seconds)
  ): ActorRef[M] = {
    val entityTypeKey: EntityTypeKey[C] =
      EntityTypeKey.apply[Command](implicitly[ClassTag[Command]].runtimeClass.getTypeName)
    val entityRef: ActorRef[M] = sharding.init(Entity(entityTypeKey)(createBehavior = { context =>
      behavior(context.entityId)(None) /*.transformMessages[M]({
        case ShardingEnvelope(entityId, message) => message
      })*/
    }))
    entityRef
  }
  private val shardActor = init

  import akka.actor.typed.scaladsl.AskPattern._
  def ask[Res](id: String)(replyTo: ActorRef[Res] => Command) =
    shardActor.ask[Res](replyTo.andThen(command => ShardingEnvelope(id, command)))

}
