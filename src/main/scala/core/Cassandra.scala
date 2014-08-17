package core

import akka.actor._
import akka.persistence._
import com.typesafe.config.{ConfigRenderOptions, ConfigFactory}

/**
 *
 */
object Cassandra extends App {

  val config = ConfigFactory.load("cassandra")
  val system = ActorSystem("cassandraSystem", config)
  //println(system.settings.config.root().render())
  val repo = system.actorOf(CassandraRepository.props, "cassandra-repository5")
  //system.actorOf(Props(classOf[CassandraView], repo), "cassandra-view")

  //repo ! "delete"
  repo ! ListRepo
  //for(i <- 0 until 100) repo ! StoreMsg("msg: " + scala.util.Random.alphanumeric.take(5).mkString)
  repo ! AddToPool((1 to 100).map(_.toString).toList)
  repo ! TakeFromPool
  repo ! ListRepo
  //repo ! "snap"

}

trait Event
trait Command

case class ListRepo() extends Command
case class StoreMsg(msg : String) extends Command
case class AddToPool(msg : List[String]) extends Command
case class TakeFromPool() extends Command

case class PoolInitialized(items: List[String]) extends Event
case class MessageReceived_v2(msg : String, sequenceNumber : Long = -1) extends Event
case class RepoState[T](inventory : List[T])


class CassandraRepository extends PersistentActor with ActorLogging {
  override def persistenceId: String = self.path.toStringWithoutAddress

  var messages = RepoState(List.empty[Event])
  var pool = Vector.empty[String]

  private def listState = log.info("lastSequenceNr: {} size: {} State: ", lastSequenceNr, messages.inventory.size)//, messages.toString())

  def updateState(msg : Event) = {
    //if(lastSequenceNr <= msg.sequenceNumber && messages.inventory.size == lastSequenceNr - 1) {
      if(lastSequenceNr % 100000 == 0) log.info("updating state " + msg)
      messages = messages.copy(msg +: messages.inventory)
      //listState
    //}
  }

  override def receiveRecover: Receive = {
    case msg : Event => updateState(msg)
  }


  def getId(size: Int) = scala.util.Random.nextInt(size)
  override def receiveCommand: Receive = {
    case StoreMsg(msg)  => persist(MessageReceived_v2(msg.toString(), lastSequenceNr + 1)){ event =>

      //log.info("Received message {}", event)
      updateState(event)
    }
    case AddToPool(msgs)  => persist(PoolInitialized(msgs)){ event =>
      pool =  scala.util.Random.shuffle(pool ++ event.items)
    }
    case msg : Event => updateState(msg)
    case ListRepo => listState
    case TakeFromPool =>

      println(s"pool: $pool")
      val id = getId(pool.size)
      val bet = pool(id)
      pool = (pool take id) ++ (pool drop (id + 1))
      println(s"pool: $pool")
      println(s"id: $id")
      println(s"bet: $bet")
      sender() ! bet

    case "snap"                                => saveSnapshot(messages)
    case "delete" => deleteMessages(lastSequenceNr); messages = RepoState(List.empty[Event])
    case SaveSnapshotSuccess(metadata)         => println(metadata)
    case SaveSnapshotFailure(metadata, reason) => println(metadata + " - - " + reason)
    case SnapshotOffer(metadata, offeredSnapshot: RepoState[Event]) =>
      println(metadata)
      messages = offeredSnapshot
  }
}

object CassandraRepository {
  def props = Props[CassandraRepository]
  //def create() =
}

class CassandraView(persistentActor : ActorRef)  extends PersistentView with ActorLogging {
  override def viewId: String = self.path.toStringWithoutAddress

  override def persistenceId: String = "/user/cassandra-repository2"

  override def receive: Actor.Receive = {
    case msg => //persistentActor ! msg
  }
}