package part2_event_sourcing

import akka.actor.{ActorLogging, ActorSystem, Props}
import akka.persistence.PersistentActor
import com.sun.javafx.css.CssError.PropertySetError
import part2_event_sourcing.PersistentActorsExercise.Candidate.{Candidate, Candidate1, Candidate2}

object PersistentActorsExercise extends App {
  /**
    * Implement persist actor for a voting station
    * receives commands in the form of case class Vote (citizen, personal id, candidate)
    * Keep citizens voted and the poll (mapping between of candidate and the number of received votes)
    *
    * the actor must be able to recover the state
    */

  object Candidate extends Enumeration {
    type Candidate = Value

    val Candidate1, Candidate2 = Value
  }

  // Commands
  case class Vote(citizenName: String, identification: String, candidate: Candidate)

  // Events
  case class Voted(vote: Vote)
  case class AlreadyVoted(citizenName: String, identification: String)

  // Special events
  case object Shutdown
  case object PrintResults

  class Voting extends PersistentActor with ActorLogging {
    // state
    var votedCitizens: List[String] = List.empty
    var poll: Map[Candidate, Int] = Map.empty

    override def persistenceId: String = "voting"
    override def receiveCommand: Receive = {
      case Vote(citizenName, identification, candidate) => {
        log.info(s"Vote command received: $citizenName, $identification, $candidate")
        if (votedCitizens.contains(identification)) {
          self ! AlreadyVoted(citizenName, identification)
        } else {
          persist(Voted(Vote(citizenName, identification, candidate))) { e =>
            votedCitizens = identification :: votedCitizens
            poll = poll + (candidate -> (poll.getOrElse(candidate, 0) + 1))
          }
        }

      }

      case PrintResults =>
        log.info(s"Results: $poll")
        log.info(s"Voted Citizens: $votedCitizens")
      case Shutdown => context.stop(self)
      case AlreadyVoted(citizenName, _) =>
        log.info(s"Citizen $citizenName already voted.")
    }

    override def receiveRecover: Receive = {
      case Voted(vote) =>
        votedCitizens = vote.identification :: votedCitizens
        poll = poll + (vote.candidate -> (poll.getOrElse(vote.candidate, 0) + 1))
    }
  }

  val system = ActorSystem("PsrsistentActorExercise1")
  val voting = system.actorOf(Props[Voting], "voting")
/*
  voting ! Vote("test1", "test1", Candidate1)
  voting ! Vote("test2", "test2", Candidate1)
  voting ! Vote("test1", "test1", Candidate2)
  voting ! Vote("test3", "test3", Candidate2)
*/
  voting ! PrintResults

  Thread.sleep(1000 * 10)
  voting ! Shutdown

  system.terminate()

}
