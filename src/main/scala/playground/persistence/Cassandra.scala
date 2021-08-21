package playground.persistence

import akka.actor.{ActorSystem, Props}
import com.typesafe.config.ConfigFactory

object Cassandra extends App {
  val actorSystem = ActorSystem("PersistenceActorSystem", ConfigFactory.load().getConfig("cassandraDemo"))
  val persistentActor = actorSystem.actorOf(Props[SimplePersistentActor], "cassandraActor")

  for(i <- 1 to 10) {
    persistentActor ! s"Message # $i"
  }

  persistentActor ! "print"
  persistentActor ! "snap"

  for(i <- 11 to 20) {
    persistentActor ! s"Message # $i"
  }

}
