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


class AkkaSchedulerSpec extends Specification with NoTimeConversions {def is = s2"""$sequential
  ${"Akka actor scheduler for RxJava".title}

  Immediate scheduling a simple task should execute that task immediately,   ${akka().e1}
    even when immediately unsubscribing.                                     ${akka().e2}

  Delayed scheduling without delay should execute immediately,               ${akka().e3}
    even when immediately unsubscribing.                                     ${akka().e4}
"""

  case class akka() extends TestKit(ActorSystem()) with After {
    val veryQuickly = 10.milliseconds

    val scheduler = new AkkaScheduler(system, Some("test"))

    def e1 = this {
      scheduler schedule (testActor ! "ping")
      testActor should receive(veryQuickly)("ping")
    }

    def e2 = this {
      val subscription = scheduler schedule (testActor ! "ping")
      subscription.unsubscribe()
      testActor should receive(veryQuickly)("ping")
    }

    def e3 = this {
      scheduler schedule (() => testActor ! "ping", 0L, TimeUnit.MILLISECONDS)
      testActor should receive(veryQuickly)("ping")
    }

    def e4 = this {
      val subscription = scheduler schedule (() => testActor ! "ping", 0L, TimeUnit.MILLISECONDS)
      subscription.unsubscribe()
      testActor should receive(veryQuickly)("ping")
    }

    override def after: Unit = {
      TestKit shutdownActorSystem system
    }

    def receive(patience: Duration): AnyRef => Matcher[ActorRef] = beSome(_) ^^ { (_: ActorRef) =>
      Option(receiveOne(patience)) // wtf, this thing seriously returns null
    }
  }
}
