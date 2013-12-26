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

import akka.actor.{ActorSystem, ActorRefFactory, Props, Actor}
import rx.lang.scala.{Subscription, Observer, Observable}
import scala.reflect.ClassTag
import scala.reflect.classTag

/** Factory for observables created from various Akka event sources.
  */
object AkkaObservables {
  def fromEventStream[T: ClassTag](system: ActorSystem, parent: Option[ActorRefFactory] = None): Observable[T] = Observable { observer =>
    val context = parent getOrElse system
    val listener = context.actorOf(Props(classOf[Listener[T]], observer))
    system.eventStream subscribe (listener, classTag[T].runtimeClass)

    Subscription {
      system.eventStream unsubscribe listener
      context stop listener
    }
  }
}

/** Factory for observables created from various Akka event sources,
  * especially for the use from within Java code.
  */
object AkkaJavaObservables {
  def fromEventStream[T: ClassTag](system: ActorSystem, parent: ActorRefFactory): rx.Observable[_ <: T] =
    AkkaObservables.fromEventStream[T](system, Some(parent)).asJavaObservable

  def fromEventStream[T: ClassTag](system: ActorSystem): rx.Observable[_ <: T] =
    AkkaObservables.fromEventStream[T](system).asJavaObservable
}

private[observables] class Listener[T](observer: Observer[T]) extends Actor {
  def receive: Receive = {
    // we know that event must have type T here
    case event ⇒ observer onNext event.asInstanceOf[T]
  }
}
