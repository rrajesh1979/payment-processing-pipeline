package playground.persistence

import akka.actor.ActorLogging
import akka.persistence._

class SimplePersistentActor extends PersistentActor with ActorLogging {
  var nMessages = 0

  override def receiveRecover: Receive = {
    case RecoveryCompleted =>
      log.info("Recovery completed")
    case SnapshotOffer(_, payload: Int) =>
      log.info("Recovered snapshot :: {}", payload)
    case message =>
      log.info("Recovered message :: {}", message)
      nMessages += 1
  }

  override def receiveCommand: Receive = {
    case "print" =>
      log.info("I have persisted {} so far.", nMessages)

    case message =>
      persist(message) {
          _ => log.info("Persisting message {}", message)
          nMessages += 1
      }

    case "snap" =>
      log.info("Saving snapshot of state")
      saveSnapshot(nMessages)

    case SaveSnapshotSuccess(metadata) =>
      log.info("Snapshot save was successful :: {}", metadata)

    case SaveSnapshotFailure(_, cause) =>
      log.info("Save snapshot failed :: {}", cause)
  }

  override def persistenceId: String = "postgre-actor"
}
