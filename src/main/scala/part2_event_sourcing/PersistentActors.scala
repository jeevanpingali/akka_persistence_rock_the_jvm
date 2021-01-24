package part2_event_sourcing

import akka.actor.{ActorLogging, ActorSystem, Props}
import akka.persistence.PersistentActor

import java.util.Date

object PersistentActors extends App {

  // COMMANDS
  case class Invoice(recipient: String, date: Date, amount: Int)

  // EVENTS
  case class InvoiceRecorded(id: Int, recipient: String, date: Date, amount: Int)

  class Accountant extends PersistentActor with ActorLogging {
    var latestInvoiceId = 0
    var totalAmount = 0
    override def persistenceId: String = "simple-accountant"

    override def receiveCommand: Receive = {
      case Invoice(recipient, date, amount) => {
        /*
        When you receibe a command
        1) you create an EVENT to persist into the store
        2) you persist this event, the pass in a callback that will get triggered once the event is written
        3) we update the actor's state when the event has persisted
         */

        log.info(s"Receive invoice for amount: $amount")
        persist(InvoiceRecorded(latestInvoiceId, recipient, date, amount)) {e =>
          latestInvoiceId += 1
          totalAmount += amount
          log.info(s"Persisted $e as invoice${e.id}, for total amount $totalAmount")
        }
      }
    }

    override def receiveRecover: Receive = {
      case InvoiceRecorded(id, _, _, amount) =>
        latestInvoiceId = id
        totalAmount += amount
        log.info(s"Recovered invoice #$id for amount $amount, total amount: $totalAmount")
    }
  }

  val system = ActorSystem("PersistentActors")
  val accountant = system.actorOf(Props[Accountant], "simpleAccountant")

/*  for(i <- 1 to 10) {
    accountant ! Invoice("The Sofa company", new Date(), i * 1000)
  }*/
}
