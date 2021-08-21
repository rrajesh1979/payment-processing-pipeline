package playground.persistence

import akka.actor.{ActorLogging, ActorSystem, Props}
import akka.persistence.{PersistentActor, SaveSnapshotFailure, SaveSnapshotSuccess, SnapshotOffer}
import com.typesafe.config.ConfigFactory

import scala.collection.mutable

object Snapshots extends App {
  val MAX_MESSAGES = 10

  var commandsWithoutCheckpoint = 0
  var currentMessageId = 0
  val lastMessages = new mutable.Queue[(String, String)]()

  // commands
  case class ReceivedMessage(contents: String) // message FROM your contact
  case class SentMessage(contents: String) // message TO your contact

  // events
  case class ReceivedMessageRecord(id: Int, contents: String)
  case class SentMessageRecord(id: Int, contents: String)

  object Chat {
    def props(owner: String, contact: String): Props = Props(new Chat(owner, contact))
  }
  class Chat(owner: String, contact: String) extends PersistentActor with ActorLogging {
    override def persistenceId: String = s"$owner-$contact-chat"

    override def receiveCommand: Receive = {
      case ReceivedMessage(contents) =>
        persist(ReceivedMessageRecord(currentMessageId, contents)) { e =>
          log.info(s"Received message: $contents")
          maybeReplaceMessage(contact, contents)
          currentMessageId += 1
          maybeCheckpoint()
        }
      case SentMessage(contents) =>
        persist(SentMessageRecord(currentMessageId, contents)) { e =>
          log.info(s"Sent message: $contents")
          maybeReplaceMessage(owner, contents)
          currentMessageId += 1
          maybeCheckpoint()
        }
      case "print" =>
        log.info(s"Most recent messages: $lastMessages")
      // snapshot-related messages
      case SaveSnapshotSuccess(metadata) =>
        log.info(s"saving snapshot succeeded: $metadata")
      case SaveSnapshotFailure(metadata, reason) =>
        log.warning(s"saving snapshot $metadata failed because of $reason")
    }

    override def receiveRecover: Receive = {
      case ReceivedMessageRecord(id, contents) =>
        log.info(s"Recovered received message $id: $contents")
        maybeReplaceMessage(contact, contents)
        currentMessageId = id
      case SentMessageRecord(id, contents) =>
        log.info(s"Recovered sent message $id: $contents")
        maybeReplaceMessage(owner, contents)
        currentMessageId = id
      case SnapshotOffer(metadata, contents) =>
        log.info(s"Recovered snapshot: $metadata")
        contents.asInstanceOf[mutable.Queue[(String, String)]].foreach(lastMessages.enqueue(_))
    }

    def maybeReplaceMessage(sender: String, contents: String): Unit = {
      if (lastMessages.size >= MAX_MESSAGES) {
        lastMessages.dequeue()
      }
      lastMessages.enqueue((sender, contents))
    }

    def maybeCheckpoint(): Unit = {
      commandsWithoutCheckpoint += 1
      if (commandsWithoutCheckpoint >= MAX_MESSAGES) {
        log.info("Saving checkpoint...")
        saveSnapshot(lastMessages)  // asynchronous operation
        commandsWithoutCheckpoint = 0
      }
    }

  }

  val system = ActorSystem("SnapshotsDemo", ConfigFactory.load().getConfig("cassandraDemo"))
  val chat = system.actorOf(Chat.props("john", "jane"))

  for (i <- 1 to 10) {
    chat ! ReceivedMessage(s"Akka Rocks $i")
    chat ! SentMessage(s"Akka Rules $i")
  }

  chat ! "print"
}
