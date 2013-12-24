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
package rx.schedulers.actor

import akka.actor.Actor
import rx.util.functions.Func2
import rx.schedulers.AkkaScheduler
import rx.schedulers.actor.SchedulerActor._
import scala.concurrent.duration.FiniteDuration

object SchedulerActor {
  type Action[T] = Func2[_ >: rx.Scheduler, _ >: T, _ <: rx.Subscription]

  sealed trait Message
  case class StatefulAction[T](state: T, action: Action[T]) extends Message
  case class Delayed[T](message: StatefulAction[T], delay: FiniteDuration) extends Message
  case class Periodic[T](message: StatefulAction[T], initialDelay: FiniteDuration, period: FiniteDuration) extends Message
  case object Cancel extends Message
}

class SchedulerActor(rxScheduler: AkkaScheduler) extends Actor {
  import context.dispatcher

  def receive: Receive = {
    case StatefulAction(state, action) => action.call(rxScheduler, state)
    case Delayed(message, delay) => sender ! context.system.scheduler.scheduleOnce(delay, self, message)
    case Periodic(message, initialDelay, period) => sender ! context.system.scheduler.schedule(initialDelay, period, self, message)
  }
}
