# rxjava-akka

Attempt at a bridge from *[RxJava](https://github.com/Netflix/RxJava)* to *[Akka](http://akka.io)* and vice versa.

## Dependency

This module is published on *[Bintray](https://bintray.com/)*.

### sbt

This is how you declare the Bintray resolver and add the dependency on `rxjava-akka` for [sbt](http://scala-sbt.org):

```Scala
resolvers += "bintray-jmhofer" at "http://dl.bintray.com/jmhofer/maven"

libraryDependencies += "de.johoop" %% "rxjava-akka" % "1.0.0"
```

### Maven

Add the repository to Maven:

```XML
<repository>
  <id>bintray-jmhofer</id>
  <url>http://dl.bintray.com/jmhofer/maven</url>
</repository>
```

Resolve the library:

```XML
<dependency>
  <groupId>de.johoop</groupId>
  <artifactId>rxjava-akka_2.10</artifactId>
  <version>1.0.0</version>
 </dependency>
```

## Usage

### Akka Scheduler

The Akka Scheduler enables you to run your observables backed by Akka actors. In order to use it, you need a context
that is able to create actors, as the scheduler will create one internally. This can either be the actor system
directly, or the context of another actor (recommended).

The context will act as parent to the created actor. You can supervise it from there in order to handle errors.

#### Sample Code

Here's a simple usage sample application (in Scala, although the scheduler works from Java, too):

```Scala
import akka.actor.{Props, ActorLogging, Actor, ActorSystem}
import akka.pattern.ask
import akka.util.Timeout

import rx.lang.scala._
import rx.schedulers.AkkaScheduler

import scala.concurrent.duration._

import RxActor._

object Main extends App {
  val system = ActorSystem("demo")

  implicit val timeout = Timeout(10 seconds) // for waiting for the Done message
  import system.dispatcher

  val rxContext = system.actorOf(Props[RxActor], "rx-context")
  rxContext ! Start
  rxContext ? Schedule onComplete (_ => system.shutdown())
}

object RxActor {
  sealed trait Message
  case object Start extends Message
  case object Schedule extends Message
  case object Done extends Message
}

class RxActor extends Actor with ActorLogging {
  def receive: Receive = {
    case Start =>
      log info "Starting..."
      val scheduler = AkkaScheduler forParentWithName (context, "rx-scheduler")
      context become scheduling(scheduler)
  }

  def scheduling(scheduler: AkkaScheduler): Receive = {
    case Schedule =>
      log info "Scheduling..."
      val ref = sender
      val observable = Observable interval (1 second, Scheduler(scheduler)) take 5
      observable subscribe (
        onNext = (tick: Long) => log info s"Tick: $tick",
        onError = (e: Throwable) => log error s"Uh-oh: $e",
        onCompleted = () => ref ! Done)
  }
}
```

### Event Stream Observables

`AkkaObservables` allows for easy creation of observables from the Akka `EventStream`.

#### Sample Code

```Scala
import akka.actor.{Props, ActorLogging, Actor, ActorSystem}

import rx.lang.scala._
import rx.observables.AkkaObservables

import RxActor._

object Main extends App {
  val system = ActorSystem("demo")
  val rxContext = system.actorOf(Props[RxActor], "rx-context")
  Seq(Start, Publish) foreach (rxContext !)
}

object RxActor {
  sealed trait Message
  case class Tick(value: Int) extends Message
  case object Start extends Message
  case object Publish extends Message
}

class RxActor extends Actor with ActorLogging {
  def receive: Receive = {
    case Start =>
      log info "Starting..."
      val observable = AkkaObservables.fromEventStream[Tick](context.system, parent = Some(context))
      context become publishingTo(observable)
  }

  def publishingTo(observable: Observable[Tick]): Receive = {
    case Publish =>
      log info "Publishing..."
      observable take 5 subscribe (
        onNext = (tick: Tick) => log info tick.toString,
        onError = (t: Throwable) => log error s"oops: $t",
        onCompleted = () => context.system.shutdown())

      1 to 10 map Tick foreach context.system.eventStream.publish
  }
}
```

## Building

This build uses *[sbt](http://scala-sbt.org)*.

## License

See [LICENSE](https://github.com/jmhofer/rxjava-akka/blob/master/LICENSE) (Apache License 2.0).
