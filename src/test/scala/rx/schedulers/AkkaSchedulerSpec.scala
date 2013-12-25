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

import akka.actor.{ActorRef, ActorSystem}
import akka.testkit.TestKit
import org.specs2.Specification
import org.specs2.specification.After
import org.specs2.time.NoTimeConversions
import rx.lang.scala.ImplicitFunctionConversions._
import scala.concurrent.duration._
import org.specs2.matcher.Matcher
import java.util.concurrent.TimeUnit
import rx.Subscription


class AkkaSchedulerSpec extends Specification with NoTimeConversions {def is = s2"""$sequential
  ${"Akka actor scheduler for RxJava".title}

  Immediate scheduling a simple task should execute that task immediately,   ${akka().e1}
    even when immediately unsubscribing.                                     ${akka().e2}

  Scheduling multiple tasks at once should not execute them concurrently.    ${akka().e13}

  Delayed scheduling without delay should execute immediately,               ${akka().e3}
    even when immediately unsubscribing.                                     ${akka().e4}

  Delayed scheduling with a short delay should
    not execute before that delay,                                           ${akka().e5}
    execute after that delay,                                                ${akka().e6}
      but not when immediately unsubscribing.                                ${akka().e7}

  Periodic scheduling should
    not execute before the initial delay,                                    ${akka().e8}
    execute after that delay,                                                ${akka().e9}
      but not when immediately unsubscribing,                                ${akka().e10}
    execute repeatedly as expected, but not anymore after unsubscribing.     ${akka().e11}
"""

  case class akka() extends TestKit(ActorSystem()) with After with Scheduling with Immediate with Delayed with Periodic {
    override def after: Unit = {
      TestKit shutdownActorSystem system
    }
  }

  trait Immediate { _: Scheduling with TestKit with After =>
    def e1 = this {
      withSubscription(scheduler schedule pingAction) {
        testActor should receive(veryQuickly)("ping")
      }
    }

    def e2 = this {
      withSubscription(scheduler schedule pingAction) {}
      testActor should receive(veryQuickly)("ping")
    }

    def e13 = this {
      withSubscription(scheduler schedule longRunningAction) {
        withSubscription(scheduler schedule pingAction) {
          testActor should receive(quickly)("long") and
            (testActor should receive(veryQuickly)("ping"))
        }
      }
    }
  }

  trait Delayed { _: Scheduling with TestKit with After =>
    def e3 = this {
      withSubscription(scheduler schedule (pingAction, 0L, TimeUnit.MILLISECONDS)) {
        testActor should receive(veryQuickly)("ping")
      }
    }

    def e4 = this {
      withSubscription(scheduler schedule (pingAction, 0L, TimeUnit.MILLISECONDS)) {}
      testActor should receive(veryQuickly)("ping")
    }

    def e5 = this {
      withSubscription(scheduler schedule (pingAction, 20L, TimeUnit.MILLISECONDS)) {
        testActor should not(receive(veryQuickly)("ping"))
      }
    }

    def e6 = this {
      withSubscription(scheduler schedule (pingAction, 20L, TimeUnit.MILLISECONDS)) {
        testActor should receive(quickly)("ping")
      }
    }

    def e7 = this {
      withSubscription(scheduler schedule (pingAction, 20L, TimeUnit.MILLISECONDS)) {}
      testActor should not(receive(quickly)("ping"))
    }
  }

  trait Periodic { _: Scheduling with TestKit with After =>
    def e8 = this {
      withSubscription(scheduler schedulePeriodically (pingAction, 20L, 1000L, TimeUnit.MILLISECONDS)) {
        testActor should not(receive(veryQuickly)("ping"))
      }
    }

    def e9 = this {
      withSubscription(scheduler schedulePeriodically (pingAction, 20L, 1000L, TimeUnit.MILLISECONDS)) {
        testActor should receive(quickly)("ping")
      }
    }

    def e10 = this {
      withSubscription(scheduler schedulePeriodically (pingAction, 20L, 1000L, TimeUnit.MILLISECONDS)) {}
      testActor should not(receive(quickly)("ping"))
    }

    def e11 = this {
      withSubscription(scheduler schedulePeriodically (pingAction, 0L, 40L, TimeUnit.MILLISECONDS)) {
        testActor should receive(quickly)("ping") and
          (testActor should receive(quickly)("ping"))
      } and (testActor should not(receive(quickly)("ping")))
    }
  }

  trait Scheduling { _: TestKit =>
    val veryQuickly: FiniteDuration = 10.milliseconds
    val quickly: FiniteDuration = 100.milliseconds

    val scheduler = new AkkaScheduler(system, Some("test"))
    val pingAction = () => testActor ! "ping"
    val longRunningAction = () => {
      Thread sleep 50
      testActor ! "long"
    }

    def receive(patience: Duration): AnyRef => Matcher[ActorRef] = beSome(_) ^^ { (_: ActorRef) =>
      Option(receiveOne(patience)) // wtf, this thing seriously returns null
    }

    def withSubscription[T](subscription: Subscription)(op: => T): T = {
      val t = op
      subscription.unsubscribe()
      t
    }
  }
}
