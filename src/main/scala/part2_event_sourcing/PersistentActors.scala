package part2_event_sourcing

import akka.actor.{ActorLogging, ActorSystem, Props}
import akka.persistence.PersistentActor

import java.util.Date

object PersistentActors extends App {

  // COMMANDS
  case class Invoice(recipient: String, date: Date, amount: Int)
  case class InvoiceBulk(invoices: List[Invoice])

  // Special Messages
  case object Shutdown

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

      case InvoiceBulk(invoices) => {
        // create events
        // persist all the events
        // update the actor state when each event is persisted
        val invoiceIds = latestInvoiceId to (latestInvoiceId + invoices.size)
        val events = invoices.zip(invoiceIds).map{pair =>
          val id = pair._2
          val invoice = pair._1
          InvoiceRecorded(id, invoice.recipient, invoice.date, invoice.amount)
        }

        persistAll(events) { e =>
          latestInvoiceId += 1
          totalAmount += e.amount
          log.info(s"Persisted SINGLE $e as invoice${e.id}, for total amount $totalAmount")
        }
      }

      case Shutdown => context.stop(self)
    }

    override def receiveRecover: Receive = {
      case InvoiceRecorded(id, _, _, amount) =>
        latestInvoiceId = id
        totalAmount += amount
        log.info(s"Recovered invoice #$id for amount $amount, total amount: $totalAmount")
    }

    /*
      * This mehtod is called if persistence failed.
      * The actor will be STOPPED
      *
      * Best practice, start the actor again, use Backoff supervisor
      */
    override def onPersistFailure(cause: Throwable, event: Any, seqNr: Long): Unit = {
      log.error(s"Failed to persist event because of $cause")
      super.onPersistFailure(cause, event, seqNr)
    }

    /*
      Called if the JOURNAL fails persist the event
      The actor is RESUMED
     */
    override def onPersistRejected(cause: Throwable, event: Any, seqNr: Long): Unit = {
      log.error(s"Persistence rejected becasue of $cause")
      super.onPersistRejected(cause, event, seqNr)
    }
  }

  val system = ActorSystem("PersistentActors")
  val accountant = system.actorOf(Props[Accountant], "simpleAccountant")

/*  for(i <- 1 to 10) {
    accountant ! Invoice("The Sofa company", new Date(), i * 1000)
  }*/

  /*
  Persistence failures
   */

  /*
  Persisting multiple events

  persistAll
   */

  val newInvoices = for(i <- 1 to 5) yield Invoice ("The awesome chair", new Date(), i * 2000)
//  accountant ! InvoiceBulk(newInvoices.toList)

  /*
    NEVER EVER CALL PERSIST OR PERSISTALL FROM FUTURES
   */

  /*
    Shutdown of persist actors
   */

  accountant ! Shutdown
}
