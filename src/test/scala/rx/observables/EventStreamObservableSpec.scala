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
package rx.observables

import akka.actor.ActorSystem
import akka.testkit.TestKit
import org.specs2.mock._
import org.specs2.Specification
import org.specs2.specification.After
import org.specs2.time.NoTimeConversions
import rx.lang.scala.{Subscription, Observable, Observer}
import scala.concurrent.blocking

class EventStreamObservableSpec extends Specification with NoTimeConversions {def is = s2"""$sequential
  ${"Event Stream Observables".title}

  An observable created from the event stream should
    not receive events posted to it until subscribed, ${akka().e1}
    receive all events posted to it,                  ${akka().e2}
    no events anymore after unsubscribing.            ${akka().e3}
"""

  case class akka() extends TestKit(ActorSystem()) with After with Mockito {

    case class Tick(tick: Int)

    val observer = mock[rx.Observer[Tick]] // mocking only works on the Java object
    val observable = AkkaObservables.fromEventStream[Tick](system)
    def subscribe = observable subscribe Observer(observer)
    def publish(tick: Int) = system.eventStream publish Tick(tick)

    def e1 = this {
      publish(1)
      subscribed(subscribe) {}
      (there was no(observer).onNext(any[Tick])) and thereWasNoErrorOrCompletionEvent
    }

    def e2 = this {
      subscribed(subscribe) {
        Seq(1, 2) foreach publish
        blocking { Thread sleep 50L } // wait a bit for the events to come through
      }
      (there was one(observer).onNext(Tick(1)) andThen one(observer).onNext(Tick(2))) and thereWasNoErrorOrCompletionEvent
    }

    def e3 = this {
      subscribed(subscribe) {}
      publish(1)
      (there was no(observer).onNext(any[Tick])) and thereWasNoErrorOrCompletionEvent
    }

    def thereWasNoErrorOrCompletionEvent =
      (there was no(observer).onError(any[Throwable])) and (there was no(observer).onCompleted())

    override def after: Unit = {
      TestKit shutdownActorSystem system
    }
  }

  def subscribed[T](subscription: Subscription)(op: => T): T = try op finally subscription.unsubscribe
}
