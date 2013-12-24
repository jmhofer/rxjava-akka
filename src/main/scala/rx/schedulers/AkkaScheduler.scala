/*
 * Copyright 2013 Joachim Hofer
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package rx.schedulers

import rx.Scheduler
import rx.Subscription
import rx.subscriptions.Subscriptions
import rx.util.functions.{Action0, Func2}

import java.util.concurrent.TimeUnit

import akka.actor._
import akka.pattern.ask
import scala.concurrent.duration._
import scala.concurrent.Future
import akka.util.Timeout

class AkkaScheduler(context: ActorContext, actorName: Option[String] = None, timeout: FiniteDuration = 1.second) extends Scheduler {
  type Action[T] = Func2[_ >: rx.Scheduler, _ >: T, _ <: rx.Subscription]

  import SchedulerActor._
  import context.dispatcher

  val actor = actorName map (context.actorOf(Props[SchedulerActor], _)) getOrElse context.actorOf(Props[SchedulerActor])

  def shutdown(): Unit = context stop actor

  def schedule[T](state: T, action: Action[T]): Subscription = {
    actor ! StatefulAction(state, action)
    Subscriptions.empty
  }

  def schedule[T](state: T, action: Action[T], delayTime: Long, unit: TimeUnit): Subscription = {
    implicit val timeout0 = Timeout(timeout)
    val cancellable = actor ? Delayed(StatefulAction(state, action), Duration(delayTime, unit))
    subscriptionFor(cancellable.mapTo[Cancellable])
  }

  override def schedulePeriodically[T](state: T, action: Action[T], initialDelay: Long, period: Long, unit: TimeUnit): Subscription = {
    implicit val timeout0 = Timeout(timeout)
    val cancellable = actor ? Periodic[T](StatefulAction[T](state, action), Duration(initialDelay, unit), Duration(period, unit))
    subscriptionFor(cancellable.mapTo[Cancellable])
  }

  private def subscriptionFor(cancellable: Future[Cancellable]) = Subscriptions create new Action0 {
    def call(): Unit = cancellable foreach (_.cancel())
  }

  private[schedulers] object SchedulerActor {
    sealed trait Message
    case class StatefulAction[T](state: T, action: Action[T]) extends Message
    case class Delayed[T](message: StatefulAction[T], delay: FiniteDuration) extends Message
    case class Periodic[T](message: StatefulAction[T], initialDelay: FiniteDuration, period: FiniteDuration) extends Message
    case object Cancel extends Message
  }

  private[schedulers] class SchedulerActor extends Actor {
    def receive: Receive = {
      case StatefulAction(state, action) => action.call(AkkaScheduler.this, state)
      case Delayed(message, delay) => sender ! context.system.scheduler.scheduleOnce(delay, self, message)
      case Periodic(message, initialDelay, period) => sender ! context.system.scheduler.schedule(initialDelay, period, self, message)
    }
  }
}
