package infrastructure.tracing.detection.stoppages

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorSystem, Behavior}
import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import akka.stream.UniqueKillSwitch
import infrastructure.actor.ShardedActor
import infrastructure.kafka.KafkaSupport.Protocol.KafkaRequirements
import infrastructure.kafka.algebra.KafkaTransaction
import infrastructure.kafka.consumer.TransactionalSource
import infrastructure.kafka.interpreter.free.FreeTransaction
import infrastructure.tracing.detection.stoppages.Domain._

import java.util.concurrent.TimeUnit
import scala.concurrent.duration.{Duration, DurationInt, FiniteDuration}
import scala.concurrent.{ExecutionContext, Future}
import scala.language.{implicitConversions, postfixOps}

object DetectionOfUnconfirmedMessages {

  case class Unconfirmed(id: String, `not confirmed for N milliseconds`: Long)

  case class Signature(
      pendingConfirmationStream: KafkaTransaction,
      confirmationStream: KafkaTransaction
  )

  def apply(
      log: Unconfirmed => Unit,
      measures: Seq[Duration] = Seq(
        10 seconds,
        30 seconds,
        60 seconds
      ),
      from: String,
      to: String
  )(
      implicit
      clusterSharding: ClusterSharding,
      actorSystem: ActorSystem[Nothing],
      executionContext: ExecutionContext,
      kafkaRequirements: KafkaRequirements
  ): Signature = {

    class StreamHealthValidation(informIssue: Unconfirmed => Unit)
        extends ShardedActor[
          Message,
          Map[String, PendingConfirmation]
        ] {

      private implicit def durationToFiniteDuration(d: Duration): FiniteDuration =
        FiniteDuration(d.toMillis, TimeUnit.MILLISECONDS)

      def behavior(
          aggregateRoot: String
      )(inQueue: Option[Map[String, PendingConfirmation]] = None): Behavior[Message] = {
        Behaviors.withTimers { timers =>
          Behaviors.receiveMessage {

            case Message(command: ActorMessages, ref) =>
              command match {
                case Timeout(id, timeInQueue) =>
                  inQueue match {
                    case Some(inQueue) =>
                      inQueue.get(id) match {
                        case Some(nonConfirmed) =>
                          informIssue(Unconfirmed(id, timeInQueue.toMillis))
                          behavior(aggregateRoot)(Some(if (timeInQueue == measures.last) inQueue - id else inQueue))
                        case None =>
                          Behaviors.same
                      }
                    case None =>
                      Behaviors.same
                  }
                case confirmation: Confirmation =>
                  behavior(aggregateRoot)(inQueue map (_ - confirmation.fromId))
                case pending: PendingConfirmation =>
                  ref ! akka.Done
                  measures.foreach { duration =>
                    timers.startSingleTimer(Message(Timeout(pending.id, duration), ref), duration)
                  }
                  behavior(aggregateRoot)(inQueue match {
                    case Some(inqueue) => Some(inqueue + (pending.id -> pending))
                    case None => Some(Map(pending.id -> pending))
                  })
              }

          }
        }
      }
    }

    val actor = new StreamHealthValidation(log)

    implicit def adapt[A](output: (UniqueKillSwitch, Future[akka.Done])): (Some[UniqueKillSwitch], Future[akka.Done]) =
      (Some(output._1), output._2)

    Signature(
      pendingConfirmationStream = FreeTransaction(
        from = from,
        to = "",
        free_run = _ =>
          new TransactionalSource[PendingConfirmation]
            .run(from, group = "detection_of_unconfirmed_messages") { pendingConfirmation =>
              actor
                .ask[akka.Done](
                  pendingConfirmation.sendTo
                )(ref => Message(pendingConfirmation, ref))
                .map(_ => Right())
            }
      ),
      confirmationStream = FreeTransaction(
        from = to,
        to = "",
        free_run = _ =>
          new TransactionalSource[Confirmation]
            .run(to, group = "detection_of_unconfirmed_messages") { confirmation =>
              actor
                .ask[akka.Done](
                  confirmation.sendTo
                )(ref => Message(confirmation, ref))
                .map(_ => Right())
            }
      )
    )
  }
}
