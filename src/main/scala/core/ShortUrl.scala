package core

import akka.actor.ActorSystem
import akka.cluster.Cluster
import com.redis.RedisClient
import spray.http.StatusCodes
import spray.routing.PathMatchers.Rest
import spray.routing.{HttpService, Route, SimpleRoutingApp, ValidationRejection}

import scala.concurrent.Future
import scala.util.Random

/**
 *
 */
object ShortUrl extends App with ShortUrlService with SimpleRoutingApp {

  implicit val actorSystem = ActorSystem("Short-Url")
  val cluster = Cluster(actorSystem)

  actorSystem.log.info("Starting")
  startServer("0.0.0.0", actorSystem.settings.config.getInt("port"))(route)

}

trait ShortUrlService {
  self: HttpService =>
  val redisClient = new RedisClient()


  implicit def executionContext = actorRefFactory.dispatcher

  def route = Route {
    path("shorten") {
      post { ctx =>
        val r = ctx.request.entity.asString
        val url = Random.alphanumeric.take(7).mkString
        val f = Future(redisClient.set(url, r))

        f.mapTo[Boolean].map {
          case true => ctx.complete {
            s"http://localhost:8080/$url"
          }
          case _ => ctx.reject(ValidationRejection(s"Cannot save url $r"))
        }
      }
    } ~
      path(Rest) { r =>
        get { ctx =>
          Future(redisClient.get(r)).mapTo[Option[String]].map {
            case Some(result) => ctx.redirect(result, StatusCodes.MovedPermanently)
            case None => ctx.reject(ValidationRejection(s"Unknown short url $r"))
          }
        }
      }

  }

}